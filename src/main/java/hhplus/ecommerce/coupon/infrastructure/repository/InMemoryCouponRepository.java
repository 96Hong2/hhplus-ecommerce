package hhplus.ecommerce.coupon.infrastructure.repository;

import hhplus.ecommerce.coupon.domain.model.Coupon;
import hhplus.ecommerce.coupon.domain.model.DiscountType;
import hhplus.ecommerce.coupon.domain.repository.CouponRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class InMemoryCouponRepository implements CouponRepository {
    private final ConcurrentHashMap<Long, Coupon> storage = new ConcurrentHashMap<>();

    @Override
    public Coupon save(Coupon coupon) {
        storage.put(coupon.getCouponId(), coupon);
        return coupon;
    }

    @Override
    public Optional<Coupon> findById(Long couponId) {
        return Optional.ofNullable(storage.get(couponId));
    }

    @Override
    public List<Coupon> findAll() {
        return storage.values().stream()
                .sorted((c1, c2) -> c2.getCreatedAt().compareTo(c1.getCreatedAt())) // 최신순
                .collect(Collectors.toList());
    }

    @Override
    public List<Coupon> findByDiscountType(DiscountType discountType) {
        return storage.values().stream()
                .filter(coupon -> coupon.getDiscountType() == discountType)
                .sorted((c1, c2) -> c2.getCreatedAt().compareTo(c1.getCreatedAt())) // 최신순
                .collect(Collectors.toList());
    }
}
