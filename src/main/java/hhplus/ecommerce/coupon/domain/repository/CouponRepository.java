package hhplus.ecommerce.coupon.domain.repository;

import hhplus.ecommerce.coupon.domain.model.Coupon;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Coupon c WHERE c.couponId = :couponId")
    Optional<Coupon> findByIdWithLock(@Param("couponId") Long couponId);

    @Query("SELECT c FROM Coupon c WHERE c.validFrom <= :now AND c.validTo >= :now AND c.issuedCount < c.maxIssueCount")
    List<Coupon> findAvailableCoupons(@Param("now") LocalDateTime now);

    @Query("SELECT c FROM Coupon c WHERE c.issuedCount < c.maxIssueCount")
    List<Coupon> findIssuableCoupons();
}
