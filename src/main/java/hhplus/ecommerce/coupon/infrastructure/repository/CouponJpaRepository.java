package hhplus.ecommerce.coupon.infrastructure.repository;

import hhplus.ecommerce.coupon.domain.model.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CouponJpaRepository extends JpaRepository<Coupon, Long> {

    // 비관적 락으로 쿠폰 조회 (선착순 발급용)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Coupon c WHERE c.couponId = :couponId")
    Optional<Coupon> findByIdWithLock(@Param("couponId") Long couponId);

    // 현재 유효한 쿠폰 조회
    @Query("SELECT c FROM Coupon c WHERE c.validFrom <= :now AND c.validTo >= :now AND c.issuedCount < c.maxIssueCount")
    List<Coupon> findAvailableCoupons(@Param("now") LocalDateTime now);

    // 발급 가능한 쿠폰 조회
    @Query("SELECT c FROM Coupon c WHERE c.issuedCount < c.maxIssueCount")
    List<Coupon> findIssuableCoupons();
}
