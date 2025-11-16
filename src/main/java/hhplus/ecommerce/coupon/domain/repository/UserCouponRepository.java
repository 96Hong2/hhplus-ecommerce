package hhplus.ecommerce.coupon.domain.repository;

import hhplus.ecommerce.coupon.domain.model.UserCoupon;
import hhplus.ecommerce.coupon.domain.model.UserCouponStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {

    List<UserCoupon> findByUserId(Long userId);

    Optional<UserCoupon> findByUserIdAndCouponId(Long userId, Long couponId);

    @Query("SELECT uc FROM UserCoupon uc WHERE uc.userId = :userId AND uc.status = :status ORDER BY uc.issuedAt DESC")
    List<UserCoupon> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") UserCouponStatus status);

    @Query("SELECT uc FROM UserCoupon uc WHERE uc.userId = :userId AND uc.status = 'ACTIVE' ORDER BY uc.issuedAt DESC")
    List<UserCoupon> findAvailableByUserId(@Param("userId") Long userId);

    @Query("SELECT uc FROM UserCoupon uc WHERE uc.couponId = :couponId")
    List<UserCoupon> findByCouponId(@Param("couponId") Long couponId);

    @Query("SELECT COUNT(uc) FROM UserCoupon uc WHERE uc.couponId = :couponId")
    Integer countByCouponId(@Param("couponId") Long couponId);
}
