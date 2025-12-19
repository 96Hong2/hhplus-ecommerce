package hhplus.ecommerce.coupon.application.service;

import hhplus.ecommerce.common.domain.exception.CouponException;
import hhplus.ecommerce.common.event.EventPublisher;
import hhplus.ecommerce.coupon.domain.event.CouponUsedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hhplus.ecommerce.coupon.domain.model.Coupon;
import hhplus.ecommerce.coupon.domain.model.UserCoupon;
import hhplus.ecommerce.coupon.domain.repository.UserCouponRepository;
import hhplus.ecommerce.coupon.domain.model.UserCouponStatus;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserCouponService {
    private final UserCouponRepository userCouponRepository;
    private final CouponService couponService;
    private final EventPublisher eventPublisher; // 이벤트 발행

    /**
     * 일반 쿠폰 발급
     *
     * @Transactional: 중복 체크 + 발급 수 체크 + UserCoupon 저장이 원자적으로 처리되어야 함 (읽기 후 쓰기)
     */
    @Transactional
    public UserCoupon issueCoupon(Long userId, Long couponId) {
        Coupon coupon = couponService.getCouponById(couponId);

        // 중복 발급 체크
        if (userCouponRepository.findByUserIdAndCouponId(userId, couponId).isPresent()) {
            throw CouponException.couponAlreadyIssued(userId, couponId);
        }

        // 발급 수 체크
        Integer currentCount = userCouponRepository.countByCouponId(couponId);
        if (currentCount >= coupon.getMaxIssueCount()) {
            throw CouponException.couponIssueLimitExceeded(couponId);
        }

        UserCoupon userCoupon = UserCoupon.create(
                userId,
                couponId
        );

        return userCouponRepository.save(userCoupon);
    }

    /**
     * 선착순 쿠폰 발급 (동시성 제어)
     *
     * @Transactional: 중복 체크 + 발급 수 증가 + UserCoupon 저장이 원자적으로 처리되어야 함
     */
    @Transactional
    public UserCoupon issueFirstComeCoupon(Long userId, Long couponId) {
        // 중복 발급 체크
        if (userCouponRepository.findByUserIdAndCouponId(userId, couponId).isPresent()) {
            throw CouponException.couponAlreadyIssued(userId, couponId);
        }

        // Pessimistic Lock으로 Coupon 조회 및 발급 수 증가 (동시성 제어)
        Coupon coupon = couponService.getCouponByIdWithLock(couponId);
        coupon.issue();

        UserCoupon userCoupon = UserCoupon.create(
                userId,
                couponId
        );

        return userCouponRepository.save(userCoupon);
    }

    /**
     * 사용자 쿠폰 목록 조회
     */
    public List<UserCoupon> getUserCoupons(Long userId, UserCouponStatus status) {
        if (status == null) {
            return userCouponRepository.findByUserId(userId);
        }
        return userCouponRepository.findByUserIdAndStatus(userId, status);
    }

    /**
     * 쿠폰 사용 처리
     *
     * @Transactional: UserCoupon 조회 + 상태 변경 + 저장이 원자적으로 처리되어야 함
     */
    @Transactional
    public UserCoupon useCoupon(Long userCouponId, Long orderId) {
        UserCoupon userCoupon = userCouponRepository.findById(userCouponId)
                .orElseThrow(() -> CouponException.couponNotFound(userCouponId));

        if (userCoupon.getStatus() == UserCouponStatus.USED) {
            throw CouponException.couponAlreadyUsed(userCouponId);
        }

        userCoupon.use(orderId);
        UserCoupon saved = userCouponRepository.save(userCoupon);

        // 쿠폰 사용 이벤트 발행
        eventPublisher.publish(CouponUsedEvent.of(saved));

        return saved;
    }

    /**
     * 특정 쿠폰 현재 발급 수 조회
     */
    public Integer getCurrentIssueCount(Long couponId) {
        return userCouponRepository.countByCouponId(couponId);
    }
}
