package hhplus.ecommerce.coupon.domain.repository;

import hhplus.ecommerce.coupon.domain.model.Coupon;
import hhplus.ecommerce.coupon.domain.model.DiscountType;

import java.util.List;
import java.util.Optional;

public interface CouponRepository {
    Coupon save(Coupon coupon);

    Optional<Coupon> findById(Long couponId);

    List<Coupon> findAll();

    List<Coupon> findByDiscountType(DiscountType discountType);
}
