package hhplus.ecommerce.coupon.infrastructure.repository;

import hhplus.ecommerce.coupon.domain.model.UserCoupon;
import hhplus.ecommerce.coupon.domain.model.UserCouponStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserCouponJpaRepository extends JpaRepository<UserCoupon, Long> {

    // 사용자별 쿠폰 조회
    List<UserCoupon> findByUserId(Long userId);

    // 사용자 + 쿠폰으로 조회 (중복 발급 체크용)
    Optional<UserCoupon> findByUserIdAndCouponId(Long userId, Long couponId);

    // 사용자별 + 상태별 쿠폰 조회
    @Query("SELECT uc FROM UserCoupon uc WHERE uc.userId = :userId AND uc.status = :status ORDER BY uc.issuedAt DESC")
    List<UserCoupon> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") UserCouponStatus status);

    // 사용 가능한 쿠폰 조회
    @Query("SELECT uc FROM UserCoupon uc WHERE uc.userId = :userId AND uc.status = 'ACTIVE' ORDER BY uc.issuedAt DESC")
    List<UserCoupon> findAvailableByUserId(@Param("userId") Long userId);

    // 쿠폰별 발급 현황 조회
    @Query("SELECT uc FROM UserCoupon uc WHERE uc.couponId = :couponId")
    List<UserCoupon> findByCouponId(@Param("couponId") Long couponId);
}
