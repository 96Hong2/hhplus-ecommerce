package hhplus.ecommerce.unitTest.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import hhplus.ecommerce.order.application.service.PaymentService;
import hhplus.ecommerce.order.domain.model.OrderStatus;
import hhplus.ecommerce.order.presentation.controller.PaymentController;
import hhplus.ecommerce.order.presentation.dto.request.PaymentRequest;
import hhplus.ecommerce.order.presentation.dto.response.PaymentResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
// claude review : Spring Boot 3.4+에서 @MockBean이 deprecated되어 @MockitoBean으로 변경
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    @DisplayName("주문 결제 API 테스트")
    void payOrder() throws Exception {
        PaymentRequest request = new PaymentRequest();
        request.setPaymentMethod("CARD");
        request.setUsedPoints(1000L);

        PaymentResponse mockResponse = new PaymentResponse(
                1L,
                "ORD-001",
                OrderStatus.PAID,
                50000L,
                "CARD",
                LocalDateTime.now()
        );

        when(paymentService.payOrder(anyLong(), any(PaymentRequest.class)))
                .thenReturn(mockResponse);

        mockMvc.perform(post("/api/payment/{orderId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderId").value(1))
                .andExpect(jsonPath("$.data.orderStatus").value("PAID"));
    }

    @Test
    @DisplayName("주문 결제 API - 포인트 미사용")
    void payOrderWithoutPoints() throws Exception {
        PaymentRequest request = new PaymentRequest();
        request.setPaymentMethod("CARD");
        request.setUsedPoints(0L);

        PaymentResponse mockResponse = new PaymentResponse(
                1L,
                "ORD-001",
                OrderStatus.PAID,
                50000L,
                "CARD",
                LocalDateTime.now()
        );

        when(paymentService.payOrder(anyLong(), any(PaymentRequest.class)))
                .thenReturn(mockResponse);

        mockMvc.perform(post("/api/payment/{orderId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderStatus").value("PAID"));
    }
}
