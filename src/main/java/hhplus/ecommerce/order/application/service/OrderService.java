package hhplus.ecommerce.order.application.service;

import hhplus.ecommerce.common.presentation.response.PageResponse;
import hhplus.ecommerce.order.application.usecase.*;
import hhplus.ecommerce.order.domain.model.OrderStatus;
import hhplus.ecommerce.order.presentation.dto.request.OrderCreateRequest;
import hhplus.ecommerce.order.presentation.dto.request.OrderItemStatusChangeRequest;
import hhplus.ecommerce.order.presentation.dto.request.OrderStatusChangeRequest;
import hhplus.ecommerce.order.presentation.dto.response.OrderCreateResponse;
import hhplus.ecommerce.order.presentation.dto.response.OrderDetailResponse;
import hhplus.ecommerce.order.presentation.dto.response.OrderItemResponse;
import hhplus.ecommerce.order.presentation.dto.response.OrderListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final CreateOrderUseCase createOrderUseCase;
    private final GetOrderListUseCase getOrderListUseCase;
    private final GetOrderDetailUseCase getOrderDetailUseCase;
    private final ChangeOrderStatusUseCase changeOrderStatusUseCase;
    private final ChangeOrderItemStatusUseCase changeOrderItemStatusUseCase;

    /**
     * 주문 생성
     * @param userId 사용자 ID
     * @param request 주문 생성 요청 (상품 옵션, 수량, 사용 포인트, 쿠폰 ID)
     * @return 주문 생성 응답
     */
    public OrderCreateResponse createOrder(Long userId, OrderCreateRequest request) {
        return createOrderUseCase.execute(userId, request);
    }

    /**
     * 유저의 주문 목록 조회
     * @param userId 사용자 ID
     * @param status 주문 상태 필터
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 주문 목록 (페이징)
     */
    public PageResponse<OrderListResponse> getOrderList(Long userId, OrderStatus status, int page, int size) {
        return getOrderListUseCase.execute(userId, status, page, size);
    }

    /**
     * 주문 상세 조회
     * @param orderId 주문 ID
     * @return 주문 상세 정보 (주문항목 포함)
     */
    public OrderDetailResponse getOrderDetail(Long orderId) {
        return getOrderDetailUseCase.execute(orderId);
    }

    /**
     * 주문 상태 변경
     * @param userId 사용자 ID
     * @param request 주문 상태 변경 요청 (주문 ID, 변경할 상태)
     * @return 변경된 주문 상세 정보
     */
    public OrderDetailResponse changeOrderStatus(Long userId, OrderStatusChangeRequest request) {
        return changeOrderStatusUseCase.execute(userId, request);
    }

    /**
     * 주문 항목 상태 변경
     * @param orderItemId 주문 항목 ID
     * @param request 주문 항목 상태 변경 요청
     * @return 변경된 주문 항목 정보
     */
    public OrderItemResponse changeOrderItemStatus(Long orderItemId, OrderItemStatusChangeRequest request) {
        return changeOrderItemStatusUseCase.execute(orderItemId, request);
    }

}

