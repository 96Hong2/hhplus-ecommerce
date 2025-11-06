package hhplus.ecommerce.coupon.domain.model;

import hhplus.ecommerce.common.domain.exception.CouponException;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

@Getter
public class Coupon {

    private static AtomicLong sequence = new AtomicLong(1);

    private final Long couponId;
    private final String couponName;
    private final DiscountType discountType;
    private final BigDecimal discountValue;
    private final BigDecimal minOrderAmount;
    private final int maxIssueCount;
    private int issuedCount;
    private final LocalDateTime validFrom;
    private final LocalDateTime validTo;
    private final Long createdBy;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Coupon(Long couponId, String couponName, DiscountType discountType, BigDecimal discountValue,
                   BigDecimal minOrderAmount, int maxIssueCount, int issuedCount,
                   LocalDateTime validFrom, LocalDateTime validTo, Long createdBy) {
        this.couponId = couponId;
        this.couponName = couponName;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.minOrderAmount = minOrderAmount;
        this.maxIssueCount = maxIssueCount;
        this.issuedCount = issuedCount;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 쿠폰을 생성한다.
     * @param couponName 쿠폰명
     * @param discountType 할인 타입
     * @param discountValue 할인 값 (정액: 금액, 정률: 퍼센트)
     * @param minOrderAmount 최소 주문 금액
     * @param maxIssueCount 최대 발급 수량
     * @param validFrom 유효기간 시작
     * @param validTo 유효기간 종료
     * @param createdBy 생성자 ID
     * @return 생성된 쿠폰
     */
    public static Coupon create(String couponName, DiscountType discountType, BigDecimal discountValue,
                                BigDecimal minOrderAmount, int maxIssueCount,
                                LocalDateTime validFrom, LocalDateTime validTo, Long createdBy) {
        validateCouponName(couponName);
        validateDiscountType(discountType);
        validateDiscountValue(discountType, discountValue);
        validateMinOrderAmount(minOrderAmount);
        validateMaxIssueCount(maxIssueCount);
        validateValidPeriod(validFrom, validTo);
        validateCreatedBy(createdBy);

        Long id = sequence.getAndIncrement();
        return new Coupon(id, couponName.trim(), discountType, discountValue,
                minOrderAmount, maxIssueCount, 0, validFrom, validTo, createdBy);
    }

    /**
     * 쿠폰을 발급한다. (선착순)
     * @return 발급 성공 여부
     */
    public boolean issue() {
        if (!canIssue()) {
            throw CouponException.couponIssueLimitExceeded(this.couponId);
        }
        this.issuedCount++;
        this.updatedAt = LocalDateTime.now();
        return true;
    }

    /**
     * 쿠폰 발급이 가능한지 확인한다.
     * @return 발급 가능 여부
     */
    public boolean canIssue() {
        return this.issuedCount < this.maxIssueCount;
    }

    /**
     * 쿠폰이 현재 유효한지 확인한다.
     * @return 유효 여부
     */
    public boolean isValid() {
        LocalDateTime now = LocalDateTime.now();
        return !now.isBefore(validFrom) && !now.isAfter(validTo);
    }

    /**
     * 쿠폰이 아직 유효하지 않은지 확인한다. (시작 전)
     * @return 시작 전 여부
     */
    public boolean isNotValidYet() {
        return LocalDateTime.now().isBefore(validFrom);
    }

    /**
     * 쿠폰이 만료되었는지 확인한다.
     * @return 만료 여부
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(validTo);
    }

    /**
     * 쿠폰을 사용할 수 있는지 확인한다.
     * @param orderAmount 주문 금액
     * @return 사용 가능 여부
     */
    public boolean canBeUsed(BigDecimal orderAmount) {
        if (!isValid()) {
            return false;
        }
        return orderAmount.compareTo(minOrderAmount) >= 0;
    }

    /**
     * 할인 금액을 계산한다.
     * @param orderAmount 주문 금액
     * @return 할인 금액
     */
    public BigDecimal calculateDiscount(BigDecimal orderAmount) {
        if (orderAmount.compareTo(minOrderAmount) < 0) {
            throw CouponException.couponMinOrderNotMet(minOrderAmount, orderAmount);
        }

        if (isNotValidYet()) {
            throw CouponException.couponNotValidYet(this.couponId);
        }
        if (isExpired()) {
            throw CouponException.couponExpired(this.couponId);
        }

        if (discountType == DiscountType.FIXED) {
            // 정액 할인: discountValue 그대로 반환
            return discountValue;
        } else {
            // 정률 할인: orderAmount * (discountValue / 100)
            BigDecimal discountRate = discountValue.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            return orderAmount.multiply(discountRate).setScale(0, RoundingMode.DOWN);
        }
    }

    private static void validateCouponName(String couponName) {
        if (couponName == null || couponName.trim().isEmpty()) {
            throw CouponException.couponIssueFailed("쿠폰명은 필수입니다.");
        }
    }

    private static void validateDiscountType(DiscountType discountType) {
        if (discountType == null) {
            throw CouponException.invalidCouponDiscountType("null");
        }
    }

    // 할인 값 검증 (정액: 0 이상, 정률: 0~100)
    private static void validateDiscountValue(DiscountType discountType, BigDecimal discountValue) {
        if (discountValue == null || discountValue.compareTo(BigDecimal.ZERO) < 0) {
            throw CouponException.couponIssueFailed("할인 값은 0 이상이어야 합니다.");
        }
        if (discountType == DiscountType.PERCENTAGE) {
            if (discountValue.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw CouponException.couponIssueFailed("정률 할인은 100%를 초과할 수 없습니다.");
            }
        }
    }

    private static void validateMinOrderAmount(BigDecimal minOrderAmount) {
        if (minOrderAmount == null || minOrderAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw CouponException.couponIssueFailed("최소 주문 금액은 0원 이상이어야 합니다.");
        }
    }

    private static void validateMaxIssueCount(int maxIssueCount) {
        if (maxIssueCount <= 0) {
            throw CouponException.couponIssueFailed("최대 발급 수량은 1 이상이어야 합니다.");
        }
    }

    private static void validateValidPeriod(LocalDateTime validFrom, LocalDateTime validTo) {
        if (validFrom == null || validTo == null) {
            throw CouponException.couponIssueFailed("유효기간은 필수입니다.");
        }
        if (!validFrom.isBefore(validTo)) {
            throw CouponException.couponIssueFailed("유효기간 시작일은 종료일보다 이전이어야 합니다.");
        }
    }

    private static void validateCreatedBy(Long createdBy) {
        if (createdBy == null) {
            throw CouponException.couponIssueFailed("생성자 ID는 필수입니다.");
        }
    }
}
