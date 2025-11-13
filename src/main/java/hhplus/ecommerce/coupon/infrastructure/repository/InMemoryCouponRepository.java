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
    private final java.util.concurrent.atomic.AtomicLong sequence = new java.util.concurrent.atomic.AtomicLong(1);

    @Override
    public Coupon save(Coupon coupon) {
        Long id = coupon.getCouponId();
        if (id == null) {
            id = sequence.getAndIncrement();
            try {
                java.lang.reflect.Field idField = Coupon.class.getDeclaredField("couponId");
                idField.setAccessible(true);
                idField.set(coupon, id);
            } catch (Exception e) {
                throw new RuntimeException("Failed to set couponId via reflection", e);
            }
        }
        storage.put(id, coupon);
        return coupon;
    }

    @Override
    public Optional<Coupon> findById(Long couponId) {
        return Optional.ofNullable(storage.get(couponId));
    }

    @Override
    public List<Coupon> findAll() {
        return storage.values().stream().collect(Collectors.toList());
    }

    @Override
    public List<Coupon> findByDiscountType(DiscountType discountType) {
        return storage.values().stream()
                .filter(coupon -> coupon.getDiscountType() == discountType)
                .collect(Collectors.toList());
    }
}
