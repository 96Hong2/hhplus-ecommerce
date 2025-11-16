package hhplus.ecommerce.unitTest.point.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import hhplus.ecommerce.point.application.service.PointMapper;
import hhplus.ecommerce.point.application.service.PointService;
import hhplus.ecommerce.point.domain.model.PointHistory;
import hhplus.ecommerce.point.domain.model.TransactionType;
import hhplus.ecommerce.point.presentation.controller.PointController;
import hhplus.ecommerce.point.presentation.dto.request.PointChargeRequest;
import hhplus.ecommerce.point.presentation.dto.response.PointHistoryResponse;
// PointBalanceResponse import 추가
import hhplus.ecommerce.point.presentation.dto.response.PointBalanceResponse;
import hhplus.ecommerce.user.domain.model.User;
// User 생성자 사용을 위해 UserRole import 추가
import hhplus.ecommerce.user.domain.model.UserRole;
import hhplus.ecommerce.user.domain.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
// Spring Boot 3.4+에서 @MockBean이 deprecated되어 @MockitoBean으로 변경
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PointController.class)
class PointControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PointService pointService;

    @MockitoBean
    private PointMapper pointMapper;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    @DisplayName("포인트 충전 API 테스트")
    void chargePoint() throws Exception {
        // amount는 BigDecimal 타입, User 생성자 파라미터 수정
        Long userId = 1L;
        PointChargeRequest request = new PointChargeRequest();
        request.setAmount(BigDecimal.valueOf(10000));
        request.setDescription("테스트 충전");

        User mockUser = new User(userId, "테스트유저", BigDecimal.ZERO, UserRole.CUSTOMER);
        PointHistory mockHistory = new PointHistory(
                1L,
                userId,
                BigDecimal.valueOf(10000),
                BigDecimal.valueOf(10000),
                "테스트 충전"
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(pointService.chargePoint(anyLong(), any(BigDecimal.class), anyString()))
                .thenReturn(mockHistory);
        // charge/use는 TransactionResponse를 사용
        when(pointMapper.toTransactionResponse(any()))
                .thenReturn(new hhplus.ecommerce.point.presentation.dto.response.PointTransactionResponse(
                        1L,
                        userId,
                        TransactionType.CHARGE,
                        BigDecimal.valueOf(10000),
                        BigDecimal.valueOf(10000),
                        "테스트 충전",
                        LocalDateTime.now()
                ));

        mockMvc.perform(post("/api/point/charge/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.amount").value(10000));
    }

    @Test
    @DisplayName("포인트 히스토리 조회 API 테스트(간단)")
    void getPointHistorySimple() throws Exception {
        Long userId = 1L;
        when(pointService.getPointHistory(anyLong(), any()))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/point/{userId}", userId))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("포인트 거래 내역 조회 API 테스트")
    void getPointHistory() throws Exception {
        Long userId = 1L;

        when(pointService.getPointHistory(anyLong(), any()))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/point/{userId}", userId))
                .andExpect(status().isOk());
    }
}
