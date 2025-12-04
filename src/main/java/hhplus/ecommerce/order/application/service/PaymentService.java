package hhplus.ecommerce.order.application.service;

import hhplus.ecommerce.common.application.DistributedLock;
import hhplus.ecommerce.common.domain.constants.BusinessConstants;
import hhplus.ecommerce.common.domain.exception.OrderException;
import hhplus.ecommerce.order.domain.model.Order;
import hhplus.ecommerce.order.domain.model.OrderItem;
import hhplus.ecommerce.order.domain.repository.OrderItemRepository;
import hhplus.ecommerce.order.domain.repository.OrderRepository;
import hhplus.ecommerce.order.presentation.dto.request.PaymentRequest;
import hhplus.ecommerce.order.presentation.dto.response.PaymentResponse;
import hhplus.ecommerce.order.domain.model.PaymentMethod;
import hhplus.ecommerce.point.application.service.PointService;
import hhplus.ecommerce.product.application.service.StockService;
import hhplus.ecommerce.product.domain.model.StockReservation;
import hhplus.ecommerce.product.domain.repository.StockReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final StockReservationRepository stockReservationRepository;
    private final StockService stockService;
    private final PointService pointService;

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 주문 결제
     * @param orderId 주문 ID
     * @param request 결제 요청 (결제 수단)
     * @return 결제 완료 응답
     */
    @Transactional
    public PaymentResponse payOrder(Long orderId, PaymentRequest request) {
        // 1. 주문 조회 및 상태 확인
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> OrderException.orderNotFound(orderId));

        if (!order.canPayment()) {
            throw OrderException.paymentFailed("결제 가능한 상태가 아닙니다.");
        }

        // 2. 주문 항목 조회
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
        if (orderItems.isEmpty()) {
            throw OrderException.orderItemsEmpty();
        }

        // 3. 결제 수단 파싱 및 검증
        PaymentMethod method;
        try {
            method = PaymentMethod.valueOf(request.getPaymentMethod().toUpperCase());
        } catch (Exception e) {
            throw OrderException.invalidPaymentMethod(request.getPaymentMethod());
        }

        // 4. 포인트 결제 처리 (필요 시)
        if (method == PaymentMethod.POINT) {
            BigDecimal amount = order.getFinalAmount();
            pointService.usePoint(order.getUserId(), amount, orderId, "주문 결제");
        }

        // 5. 주문 상태 업데이트 (PAID) 및 결제수단 반영
        Order paidOrder = order.payWithMethod(method);
        orderRepository.save(paidOrder);

        // 6. 재고 예약 확정 (각 주문 항목에 대해)
        for (OrderItem orderItem : orderItems) {
            StockReservation reservation = stockReservationRepository
                .findByProductOptionIdAndOrderId(orderItem.getProductOptionId(), orderId)
                .orElseThrow(() -> OrderException.paymentFailed(
                    "재고 예약을 찾을 수 없습니다. [상품옵션ID: " + orderItem.getProductOptionId() + "]"));

            stockService.confirmStockReservation(reservation.getStockReservationId());

            // Redis 인기상품 실시간 통계 점수 증가
            try {
                updateProductTopN(orderItem.getProductId().toString(), orderItem.getQuantity());
            } catch (Exception e) {
                log.info("Redis top-N update failed. productId :{}, quantity : {}, exception : {}", orderItem.getProductId().toString(), orderItem.getQuantity(), e.getMessage());
            }
        }

        // 7. 결제 결과 반환
        return new PaymentResponse(
                paidOrder.getOrderId(),
                paidOrder.getOrderNumber(),
                paidOrder.getOrderStatus(),
                paidOrder.getFinalAmount(),
                method.name(),
                LocalDateTime.now()
        );
    }

    /**
     * Redis 인기상품 점수 업데이트 (실시간)
     *
     * 키 초기화 전략
     * - daily: 매일 자정 스케줄러로 초기화 (DEL 후 재생성)
     * - weekly: 매주 월요일 자정 초기화
     * - monthly: 매월 1일 자정 초기화
     *
     * @param productId 상품 ID
     * @param quantity 주문 수량
     */
    public void updateProductTopN(String productId, int quantity) {
        String dailyKey = BusinessConstants.REDIS_TOP_N_DAILY_KEY;
        String weeklyKey = BusinessConstants.REDIS_TOP_N_WEEKLY_KEY;
        String monthlyKey = BusinessConstants.REDIS_TOP_N_MONTHLY_KEY;

        boolean hasDailyKey = redisTemplate.hasKey(dailyKey);
        boolean hasWeeklyKey = redisTemplate.hasKey(weeklyKey);
        boolean hasMonthlyKey = redisTemplate.hasKey(monthlyKey);

        // 판매량 score 증가
        redisTemplate.opsForZSet().incrementScore(dailyKey, productId, quantity);
        redisTemplate.opsForZSet().incrementScore(weeklyKey, productId, quantity);
        redisTemplate.opsForZSet().incrementScore(monthlyKey, productId, quantity);

        // 만료 설정 (스케줄러 삭제는 별도로 수행되며 방어용으로 사용)
        // - daily는 2일 후 만료 (어제/오늘 데이터 보관)
        // - weekly는 8일 후 만료
        // - monthly는 2개월 후 만료
        if (!hasDailyKey) {
            redisTemplate.expire(dailyKey, Duration.ofDays(2));
        }
        if (!hasWeeklyKey) {
            redisTemplate.expire(weeklyKey, Duration.ofDays(8));
        }
        if (!hasMonthlyKey) {
            redisTemplate.expire(monthlyKey, Duration.ofDays(60));
        }

        // 메모리 최적화 - 하위 순위 제거
        // - Top 1000위 밖의 상품은 ZREMRANGEBYRANK로 제거 (조회는 Top100이 최대임)
        redisTemplate.opsForZSet().removeRange(dailyKey, 0, -1001);
        redisTemplate.opsForZSet().removeRange(weeklyKey, 0, -1001);
        redisTemplate.opsForZSet().removeRange(monthlyKey, 0, -1001);
    }
}
