package hhplus.ecommerce.order.presentation.controller;

import hhplus.ecommerce.common.presentation.response.ApiResponse;
import hhplus.ecommerce.order.application.service.PaymentService;
import hhplus.ecommerce.order.presentation.dto.request.PaymentRequest;
import hhplus.ecommerce.order.presentation.dto.response.PaymentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payment")
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * 주문 결제
     * @param orderId 주문 ID
     * @param request 결제 요청 (결제 수단, 사용 포인트)
     * @return 결제 완료 응답 (주문 ID, 주문번호, 상태, 결제 금액, 결제 시간)
     */
    @PostMapping("/{orderId}")
    public ApiResponse<PaymentResponse> payOrder(
            @PathVariable Long orderId,
            @RequestBody PaymentRequest request) {
        PaymentResponse response = paymentService.payOrder(orderId, request);
        return ApiResponse.success(response);
    }
}
