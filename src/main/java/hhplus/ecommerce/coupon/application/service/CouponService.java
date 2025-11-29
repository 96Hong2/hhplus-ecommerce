package hhplus.ecommerce.coupon.application.service;

import hhplus.ecommerce.common.domain.exception.CouponException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import hhplus.ecommerce.coupon.domain.model.Coupon;
import hhplus.ecommerce.coupon.domain.model.DiscountType;
import hhplus.ecommerce.coupon.domain.repository.CouponRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;

    /**
     * 쿠폰 생성
     */
    public Coupon createCoupon(String couponName, DiscountType discountType, BigDecimal discountValue,
                               BigDecimal minOrderAmount, Integer maxIssueCount,
                               LocalDateTime validFrom, LocalDateTime validTo, Long createdBy) {

        // 유효기간 검증
        if (validTo.isBefore(validFrom)) {
            throw CouponException.couponCreateFail("유효 종료일이 시작일보다 이전일 수 없습니다.");
        }

        // 할인값 검증
        if (discountType == DiscountType.PERCENTAGE && discountValue.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw CouponException.couponCreateFail("정률 할인은 100% 이하여야 합니다.");
        }

        Coupon coupon = Coupon.create(
                couponName,
                discountType,
                discountValue,
                minOrderAmount,
                maxIssueCount,
                validFrom,
                validTo,
                createdBy
        );

        return couponRepository.save(coupon);
    }

    /**
     * 쿠폰 목록 조회
     */
    public List<Coupon> getCoupons(DiscountType discountType) {
        if (discountType == null) {
            return couponRepository.findAll();
        }
        return couponRepository.findByDiscountType(discountType);
    }

    /**
     * 쿠폰 ID로 조회
     * (JPA 엔티티 직렬화 문제로 캐싱 미적용)
     */
    public Coupon getCouponById(Long couponId) {
        return couponRepository.findById(couponId)
                .orElseThrow(() -> CouponException.couponCreateFail("쿠폰을 찾을 수 없습니다."));
    }

    /**
     * 쿠폰 ID로 조회 (Pessimistic Lock)
     * 동시성 제어가 필요한 경우 사용
     */
    public Coupon getCouponByIdWithLock(Long couponId) {
        return couponRepository.findByIdWithLock(couponId)
                .orElseThrow(() -> CouponException.couponCreateFail("쿠폰을 찾을 수 없습니다."));
    }

    /**
     * 쿠폰 사용 가능 여부 확인
     */
    public boolean isCouponAvailable(Coupon coupon, BigDecimal orderAmount, LocalDateTime currentTime) {
        // 유효기간 체크
        if (currentTime.isBefore(coupon.getValidFrom()) || currentTime.isAfter(coupon.getValidTo())) {
            return false;
        }

        // 최소 주문 금액 체크
        if (orderAmount.compareTo(coupon.getMinOrderAmount()) < 0) {
            return false;
        }

        return true;
    }

    /**
     * 쿠폰 할인 금액 계산
     */
    public Long calculateDiscountAmount(Coupon coupon, BigDecimal orderAmount) {
        if (coupon.getDiscountType() == DiscountType.FIXED) {
            return Math.min(coupon.getDiscountValue().longValue(), orderAmount.longValue());
        } else { // PERCENTAGE
            return orderAmount.multiply(coupon.getDiscountValue())
                    .divide(BigDecimal.valueOf(100))
                    .longValue();
        }
    }
}
