package hhplus.ecommerce.order.application.usecase;

import hhplus.ecommerce.common.domain.exception.IntegrationException;
import hhplus.ecommerce.integration.application.service.ExternalIntegrationService;
import hhplus.ecommerce.order.application.dto.OrderItemInfo;
import hhplus.ecommerce.order.application.service.OrderService;
import hhplus.ecommerce.order.domain.model.Order;
import hhplus.ecommerce.order.presentation.dto.request.OrderCreateRequest;
import hhplus.ecommerce.order.presentation.dto.response.OrderCreateResponse;
import hhplus.ecommerce.product.application.service.StockService;
import hhplus.ecommerce.product.domain.model.StockReservation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 주문 생성 UseCase - Saga 패턴 적용
 *
 * Saga 패턴을 적용하여 분산 트랜잭션을 관리합니다:
 * 1. 주문 생성 트랜잭션 (로컬 트랜잭션)
 * 2. 외부 시스템 연동 (독립적 트랜잭션)
 * 3. 실패 시 보상 트랜잭션 실행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreateOrderUseCase {

    private final OrderService orderService;
    private final StockService stockService;
    private final ExternalIntegrationService externalIntegrationService;

    /**
     * 주문 생성 Saga 오케스트레이터
     *
     * 1. 주문 생성 트랜잭션 실행
     * 2. 외부 시스템(ERP) 연동
     * 3. 외부 시스템 연동 실패 시 보상 트랜잭션 실행
     *
     * @param userId 사용자 ID
     * @param request 주문 생성 요청
     * @return 주문 생성 응답
     */
    public OrderCreateResponse execute(Long userId, OrderCreateRequest request) {
        OrderCreateResponse response = null;

        try {
            // Step 1: 주문 생성 트랜잭션 (커밋됨)
            log.info("주문 생성 트랜잭션 시작 - UserId: {}", userId);
            response = createOrderTransaction(userId, request);
            log.info("주문 생성 트랜잭션 완료 - OrderId: {}, OrderNumber: {}",
                    response.getOrderId(), response.getOrderNumber());

            // Step 2: 외부 시스템(ERP) 연동 (별도 트랜잭션)
            log.info("외부 시스템 연동 시작 - OrderId: {}", response.getOrderId());
            Order order = orderService.getOrder(response.getOrderId());
            externalIntegrationService.sendOrderToERP(order);
            log.info("외부 시스템 연동 완료 - OrderId: {}", response.getOrderId());

            return response;

        } catch (IntegrationException e) {
            // Step 3: 보상 트랜잭션 - 주문 취소 및 재고 복구
            log.error("외부 시스템 연동 실패, 보상 트랜잭션 시작 - OrderId: {}",
                    response != null ? response.getOrderId() : "N/A", e);

            if (response != null) {
                compensateOrder(response.getOrderId());
                log.info("보상 트랜잭션 완료 - OrderId: {}", response.getOrderId());
            }

            throw e;
        } catch (Exception e) {
            // 주문 생성 트랜잭션 실패 시 자동 롤백 (Spring @Transactional)
            log.error("주문 생성 실패 - UserId: {}", userId, e);
            throw e;
        }
    }

    /**
     * 주문 생성 트랜잭션
     *
     * 주문 생성, 재고 예약, 주문 아이템 저장을 하나의 트랜잭션으로 처리
     * 실패 시 모두 롤백됨
     *
     * @Transactional: 주문 저장, 재고 예약, 주문 아이템 저장이 모두 성공하거나 모두 롤백
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    protected OrderCreateResponse createOrderTransaction(Long userId, OrderCreateRequest request) {
        // 1. 주문 아이템 정보 수집 (상품 조회, 가격 계산 포함) - 배치 조회로 N+1 문제 해결
        List<OrderItemInfo> orderItemInfos = orderService.collectOrderItemsBatch(request.getItems());

        // 2. 총 주문 금액 계산
        BigDecimal totalAmount = orderService.calculateTotalAmount(orderItemInfos);

        // 3. 쿠폰 할인 금액 계산
        BigDecimal discountAmount = orderService.calculateCouponDiscount(
                request.getCouponId(),
                totalAmount
        );

        // 4. 주문 번호 생성
        String orderNumber = orderService.generateOrderNumber(userId);

        // 5. 주문 생성 (Domain 팩토리 메서드 사용)
        Order order = Order.create(
                orderNumber,
                userId,
                totalAmount,
                discountAmount,
                request.getCouponId()
        );

        // 6. 주문 저장
        Order savedOrder = orderService.saveOrder(order);

        // 7. 재고 예약
        orderService.reserveStocks(savedOrder.getOrderId(), orderItemInfos);

        // 8. 주문 아이템 생성 및 저장
        orderService.saveOrderItems(savedOrder.getOrderId(), orderItemInfos);

        // 9. 응답 생성 (Presentation DTO로 변환)
        return new OrderCreateResponse(
                savedOrder.getOrderId(),
                savedOrder.getOrderNumber(),
                savedOrder.getOrderStatus(),
                savedOrder.getTotalAmount(),
                savedOrder.getDiscountAmount(),
                savedOrder.getFinalAmount(),
                savedOrder.getExpiresAt()
        );
    }

    /**
     * 보상 트랜잭션 (Compensating Transaction)
     *
     * 외부 시스템 연동 실패 시 이미 커밋된 주문을 취소하고 재고를 복구
     *
     * 보상 작업:
     * 1. 주문 상태를 CANCELLED로 변경
     * 2. 예약된 재고 해제 (물리 재고 복구)
     *
     * @param orderId 취소할 주문 ID
     */
    @Transactional
    protected void compensateOrder(Long orderId) {
        log.info("보상 트랜잭션 실행 - OrderId: {}", orderId);

        try {
            // 1. 주문 취소
            orderService.cancelOrder(orderId);
            log.info("주문 취소 완료 - OrderId: {}", orderId);

            // 2. 재고 예약 해제 (역순으로 롤백)
            List<StockReservation> reservations = stockService.getReservationsByOrderId(orderId);
            for (StockReservation reservation : reservations) {
                stockService.releaseStockReservation(reservation.getStockReservationId());
                log.info("재고 예약 해제 - ReservationId: {}, ProductOptionId: {}, Quantity: {}",
                        reservation.getStockReservationId(),
                        reservation.getProductOptionId(),
                        reservation.getReservedQuantity());
            }

            log.info("보상 트랜잭션 성공 - OrderId: {}", orderId);

        } catch (Exception e) {
            // 일단 보상트랜잭션 실패 시는 로깅만 하도록 함
            log.error("보상 트랜잭션 실패 - OrderId: {}", orderId, e);
            throw e;
        }
    }
}
