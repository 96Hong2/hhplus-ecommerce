package hhplus.ecommerce.unitTest.coupon.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import hhplus.ecommerce.coupon.application.service.CouponMapper;
import hhplus.ecommerce.coupon.application.service.CouponService;
import hhplus.ecommerce.coupon.application.service.UserCouponService;
import hhplus.ecommerce.coupon.domain.model.Coupon;
import hhplus.ecommerce.coupon.domain.model.DiscountType;
import hhplus.ecommerce.coupon.domain.model.UserCoupon;
import hhplus.ecommerce.coupon.presentation.controller.CouponController;
import hhplus.ecommerce.coupon.presentation.dto.request.CouponCreateRequest;
import hhplus.ecommerce.coupon.presentation.dto.request.CouponIssueRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
// claude review : Spring Boot 3.4+에서 @MockBean이 deprecated되어 @MockitoBean으로 변경
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CouponController.class)
class CouponControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CouponService couponService;

    @MockitoBean
    private UserCouponService userCouponService;

    @MockitoBean
    private CouponMapper couponMapper;

    @Test
    @DisplayName("쿠폰 생성 API 테스트")
    void createCoupon() throws Exception {
        CouponCreateRequest request = new CouponCreateRequest();
        request.setCouponName("신규 쿠폰");
        request.setDiscountType(DiscountType.FIXED);
        request.setDiscountValue(BigDecimal.valueOf(10000));
        request.setMinOrderAmount(BigDecimal.valueOf(50000));
        request.setMaxIssueCount(100);
        request.setValidFrom(LocalDateTime.now());
        request.setValidTo(LocalDateTime.now().plusDays(30));
        request.setCreatedBy(1L);

        Coupon mockCoupon = Coupon.create(
                "신규 쿠폰",
                DiscountType.FIXED,
                BigDecimal.valueOf(10000),
                BigDecimal.valueOf(50000),
                100,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(30),
                1L
        );

        when(couponService.createCoupon(anyString(), any(), any(), any(), anyInt(), any(), any(), anyLong()))
                .thenReturn(mockCoupon);

        mockMvc.perform(post("/api/coupon")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("쿠폰 목록 조회 API 테스트")
    void getCoupons() throws Exception {
        when(couponService.getCoupons(any()))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/coupon"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("사용자 쿠폰 조회 API 테스트")
    void getUserCoupons() throws Exception {
        when(userCouponService.getUserCoupons(anyLong(), any()))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/coupon/user/{userId}", 1L)
                        .param("isUsed", "false"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("일반 쿠폰 발급 API 테스트")
    void issueCoupon() throws Exception {
        UserCoupon mockUserCoupon = UserCoupon.create(1L, 1L);
        Coupon mockCoupon = Coupon.create(
                "테스트 쿠폰",
                DiscountType.FIXED,
                BigDecimal.valueOf(10000),
                BigDecimal.valueOf(50000),
                100,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(30),
                1L
        );

        when(userCouponService.issueCoupon(anyLong(), anyLong()))
                .thenReturn(mockUserCoupon);
        when(couponService.getCouponById(anyLong()))
                .thenReturn(mockCoupon);

        mockMvc.perform(post("/api/coupon/user/{userId}/{couponId}", 1L, 1L))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("선착순 쿠폰 발급 API 테스트")
    void issueFirstComeCoupon() throws Exception {
        CouponIssueRequest request = new CouponIssueRequest();
        request.setUserId(1L);

        UserCoupon mockUserCoupon = UserCoupon.create(1L, 1L);
        Coupon mockCoupon = Coupon.create(
                "선착순 쿠폰",
                DiscountType.PERCENTAGE,
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(30000),
                100,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(30),
                1L
        );

        when(userCouponService.issueFirstComeCoupon(anyLong(), anyLong()))
                .thenReturn(mockUserCoupon);
        when(couponService.getCouponById(anyLong()))
                .thenReturn(mockCoupon);

        mockMvc.perform(patch("/api/coupon/{couponId}/issue", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
