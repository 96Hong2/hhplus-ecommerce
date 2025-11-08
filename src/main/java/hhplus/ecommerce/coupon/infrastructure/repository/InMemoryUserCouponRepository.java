package hhplus.ecommerce.coupon.infrastructure.repository;

import hhplus.ecommerce.coupon.domain.model.UserCoupon;
import hhplus.ecommerce.coupon.domain.repository.UserCouponRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Repository
public class InMemoryUserCouponRepository implements UserCouponRepository {
    private final ConcurrentHashMap<Long, UserCoupon> storage = new ConcurrentHashMap<>();

    // 쿠폰별 발급 수 관리: Coupon ID -> 발급 수
    private final ConcurrentHashMap<Long, AtomicInteger> issueCountMap = new ConcurrentHashMap<>();

    // 중복 발급 체크 인덱스: "userId:couponId" -> UserCoupon ID
    private final ConcurrentHashMap<String, Long> userCouponIndex = new ConcurrentHashMap<>();

    @Override
    public UserCoupon save(UserCoupon userCoupon) {
        storage.put(userCoupon.getUserCouponId(), userCoupon);

        // 발급 수 증가
        issueCountMap.computeIfAbsent(userCoupon.getCouponId(), k -> new AtomicInteger(0))
                .incrementAndGet();

        // 중복 발급 체크 인덱스 업데이트
        String indexKey = buildIndexKey(userCoupon.getUserId(), userCoupon.getCouponId());
        userCouponIndex.put(indexKey, userCoupon.getUserCouponId());

        return userCoupon;
    }

    @Override
    public Optional<UserCoupon> findById(Long userCouponId) {
        return Optional.ofNullable(storage.get(userCouponId));
    }

    @Override
    public List<UserCoupon> findByUserId(Long userId) {
        return storage.values().stream()
                .filter(userCoupon -> userCoupon.getUserId().equals(userId))
                .sorted((uc1, uc2) -> uc2.getIssuedAt().compareTo(uc1.getIssuedAt())) // 최신순
                .collect(Collectors.toList());
    }

    @Override
    public List<UserCoupon> findByUserIdAndIsUsed(Long userId, Boolean isUsed) {
        return storage.values().stream()
                .filter(userCoupon -> userCoupon.getUserId().equals(userId))
                .filter(userCoupon -> userCoupon.isUsed() == isUsed)
                .sorted((uc1, uc2) -> uc2.getIssuedAt().compareTo(uc1.getIssuedAt())) // 최신순
                .collect(Collectors.toList());
    }

    @Override
    public Optional<UserCoupon> findByUserIdAndCouponId(Long userId, Long couponId) {
        String indexKey = buildIndexKey(userId, couponId);
        Long userCouponId = userCouponIndex.get(indexKey);

        if (userCouponId == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(storage.get(userCouponId));
    }

    @Override
    public Integer countByCouponId(Long couponId) {
        AtomicInteger count = issueCountMap.get(couponId);
        return count == null ? 0 : count.get();
    }

    @Override
    public boolean incrementIssueCountIfAvailable(Long couponId, Integer maxIssueCount) {
        AtomicInteger count = issueCountMap.computeIfAbsent(couponId, k -> new AtomicInteger(0));

        // CAS(Compare-And-Swap) 방식으로 원자적으로 증가
        while (true) {
            int current = count.get();

            // 최대 발급 수 초과 체크
            if (current >= maxIssueCount) {
                return false;
            }

            // 원자적으로 증가 시도
            if (count.compareAndSet(current, current + 1)) {
                return true;
            }
            // CAS 실패 시 재시도
        }
    }

    private String buildIndexKey(Long userId, Long couponId) {
        return userId + ":" + couponId;
    }
}
