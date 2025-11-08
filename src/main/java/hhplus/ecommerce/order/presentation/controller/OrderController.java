package hhplus.ecommerce.order.presentation.controller;

import hhplus.ecommerce.common.presentation.response.ApiResponse;
import hhplus.ecommerce.common.presentation.response.PageResponse;
import hhplus.ecommerce.order.application.service.OrderService;
import hhplus.ecommerce.order.domain.model.OrderStatus;
import hhplus.ecommerce.order.presentation.dto.request.OrderCreateRequest;
import hhplus.ecommerce.order.presentation.dto.request.OrderItemStatusChangeRequest;
import hhplus.ecommerce.order.presentation.dto.request.OrderStatusChangeRequest;
import hhplus.ecommerce.order.presentation.dto.response.OrderCreateResponse;
import hhplus.ecommerce.order.presentation.dto.response.OrderDetailResponse;
import hhplus.ecommerce.order.presentation.dto.response.OrderItemResponse;
import hhplus.ecommerce.order.presentation.dto.response.OrderListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/order")
public class OrderController {

    private final OrderService orderService;

    /**
     * 주문 생성
     * @param userId 사용자 ID
     * @param request 주문 생성 요청 (상품 옵션, 수량, 사용 포인트, 쿠폰 ID)
     * @return 주문 생성 응답 (주문 ID, 주문번호, 상태, 금액, 만료시간)
     */
    @PostMapping("/{userId}")
    public ApiResponse<OrderCreateResponse> createOrder(
            @PathVariable Long userId,
            @RequestBody OrderCreateRequest request) {
        OrderCreateResponse response = orderService.createOrder(userId, request);
        return ApiResponse.success(response);
    }

    /**
     * 유저의 주문 목록 조회
     * @param userId 사용자 ID
     * @param status 주문 상태 필터 (기본값: PAID)
     * @param page 페이지 번호 (기본값: 0)
     * @param size 페이지 크기 (기본값: 20)
     * @return 주문 목록 (페이징)
     */
    @GetMapping("/{userId}")
    public PageResponse<OrderListResponse> getOrderList(
            @PathVariable Long userId,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return orderService.getOrderList(userId, status, page, size);
    }

    /**
     * 주문 상세 조회 (주문항목 목록 포함)
     * @param orderId 주문 ID
     * @return 주문 상세 정보 (주문항목, 금액, 상태 등)
     */
    @GetMapping("/detail/{orderId}")
    public ApiResponse<OrderDetailResponse> getOrderDetail(@PathVariable Long orderId) {
        OrderDetailResponse response = orderService.getOrderDetail(orderId);
        return ApiResponse.success(response);
    }

    /**
     * 주문 상태 변경
     * @param userId 사용자 ID
     * @param request 주문 상태 변경 요청 (주문 ID, 변경할 상태)
     * @return 변경된 주문 상세 정보
     */
    @PatchMapping("/status/{userId}")
    public ApiResponse<OrderDetailResponse> changeOrderStatus(
            @PathVariable Long userId,
            @RequestBody OrderStatusChangeRequest request) {
        OrderDetailResponse response = orderService.changeOrderStatus(userId, request);
        return ApiResponse.success(response);
    }

    /**
     * 주문 항목 상태 변경
     * @param orderItemId 주문 항목 ID
     * @param request 주문 항목 상태 변경 요청
     * @return 변경된 주문 항목 정보
     */
    @PatchMapping("/orderItem/status/{orderItemId}")
    public ApiResponse<OrderItemResponse> changeOrderItemStatus(
            @PathVariable Long orderItemId,
            @RequestBody OrderItemStatusChangeRequest request) {
        OrderItemResponse response = orderService.changeOrderItemStatus(orderItemId, request);
        return ApiResponse.success(response);
    }

}
