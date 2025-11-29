package hhplus.ecommerce.coupon.presentation.controller;

import hhplus.ecommerce.common.presentation.response.ApiResponse;
import hhplus.ecommerce.coupon.application.service.CouponMapper;
import hhplus.ecommerce.coupon.application.service.CouponService;
import hhplus.ecommerce.coupon.application.service.RedisCouponService;
import hhplus.ecommerce.coupon.application.service.UserCouponService;
import hhplus.ecommerce.coupon.domain.model.Coupon;
import hhplus.ecommerce.coupon.domain.model.DiscountType;
import hhplus.ecommerce.coupon.domain.model.UserCoupon;
import hhplus.ecommerce.coupon.presentation.dto.request.CouponCreateRequest;
import hhplus.ecommerce.coupon.presentation.dto.request.CouponIssueRequest;
import hhplus.ecommerce.coupon.presentation.dto.response.CouponResponse;
import hhplus.ecommerce.coupon.presentation.dto.response.UserCouponResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import hhplus.ecommerce.coupon.domain.model.UserCouponStatus;

@RestController
@RequestMapping("/api/coupon")
public class CouponController {
    private final CouponService couponService;
    private final UserCouponService userCouponService;
    private final CouponMapper couponMapper;

    @Autowired(required = false)
    private RedisCouponService redisCouponService;

    public CouponController(CouponService couponService,
                            UserCouponService userCouponService,
                            CouponMapper couponMapper) {
        this.couponService = couponService;
        this.userCouponService = userCouponService;
        this.couponMapper = couponMapper;
    }

    /**
     * 쿠폰 생성
     * POST /api/coupons
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CouponResponse> createCoupon(@Valid @RequestBody CouponCreateRequest request) {
        Coupon coupon = couponService.createCoupon(
                request.getCouponName(),
                request.getDiscountType(),
                request.getDiscountValue(),
                request.getMinOrderAmount(),
                request.getMaxIssueCount(),
                request.getValidFrom(),
                request.getValidTo(),
                request.getCreatedBy()
        );

        CouponResponse response = couponMapper.toCouponResponse(coupon);
        return ApiResponse.success(response, "쿠폰이 생성되었습니다.");
    }

    /**
     * 쿠폰 목록 조회
     * GET /api/coupons?discountType=FIXED
     */
    @GetMapping
    public ApiResponse<List<CouponResponse>> getCoupons(
            @RequestParam(required = false) DiscountType discountType) {

        List<Coupon> coupons = couponService.getCoupons(discountType);
        List<CouponResponse> responses = couponMapper.toCouponResponseList(coupons);

        return ApiResponse.success(responses);
    }

    /**
     * 사용자 쿠폰 조회
     * GET /api/coupons/user/{userId}?status=ACTIVE|USED|EXPIRED
     */
    @GetMapping("/user/{userId}")
    public ApiResponse<List<UserCouponResponse>> getUserCoupons(
            @PathVariable Long userId,
            @RequestParam(required = false) UserCouponStatus status) {

        List<UserCoupon> userCoupons = userCouponService.getUserCoupons(userId, status);
        List<UserCouponResponse> responses = couponMapper.toUserCouponResponseList(userCoupons);

        return ApiResponse.success(responses);
    }

    /**
     * 일반 쿠폰 발급
     * POST /api/coupons/user/{userId}/{couponId}
     */
    @PostMapping("/user/{userId}/{couponId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserCouponResponse> issueCoupon(
            @PathVariable Long userId,
            @PathVariable Long couponId) {

        UserCoupon userCoupon = userCouponService.issueCoupon(userId, couponId);
        Coupon coupon = couponService.getCouponById(couponId);
        UserCouponResponse response = couponMapper.toUserCouponResponse(userCoupon, coupon);

        return ApiResponse.success(response, "쿠폰이 발급되었습니다.");
    }

    /**
     * 선착순 쿠폰 발급 (DB 비관적 락)
     * PATCH /api/coupons/{couponId}/issue
     */
    @PatchMapping("/{couponId}/issue")
    public ApiResponse<UserCouponResponse> issueFirstComeCoupon(
            @PathVariable Long couponId,
            @Valid @RequestBody CouponIssueRequest request) {

        UserCoupon userCoupon = userCouponService.issueFirstComeCoupon(request.getUserId(), couponId);
        Coupon coupon = couponService.getCouponById(couponId);
        UserCouponResponse response = couponMapper.toUserCouponResponse(userCoupon, coupon);

        return ApiResponse.success(response, "선착순 쿠폰이 발급되었습니다.");
    }

    /**
     * 선착순 쿠폰 발급 (Redis SET)
     * PATCH /api/coupons/{couponId}/issue-redis
     */
    @PatchMapping("/{couponId}/issue-redis")
    public ApiResponse<UserCouponResponse> issueFirstComeCouponWithRedis(
            @PathVariable Long couponId,
            @Valid @RequestBody CouponIssueRequest request) {

        if (redisCouponService == null) {
            // Redis가 활성화되지 않은 경우 DB 락 방식으로 폴백
            return issueFirstComeCoupon(couponId, request);
        }

        UserCoupon userCoupon = redisCouponService.issueCouponWithRedis(request.getUserId(), couponId);
        Coupon coupon = couponService.getCouponById(couponId);
        UserCouponResponse response = couponMapper.toUserCouponResponse(userCoupon, coupon);

        return ApiResponse.success(response, "선착순 쿠폰이 발급되었습니다. (Redis)");
    }
}
