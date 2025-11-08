package hhplus.ecommerce.coupon.presentation.dto.request;
import hhplus.ecommerce.coupon.domain.model.DiscountType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class CouponCreateRequest {

    @NotBlank(message = "쿠폰 이름은 필수입니다")
    private String couponName;

    @NotNull(message = "할인 타입은 필수입니다")
    private DiscountType discountType;

    @NotNull(message = "할인 값은 필수입니다")
    @Min(value = 1, message = "할인 값은 1 이상이어야 합니다")
    private BigDecimal discountValue;

    @Min(value = 0, message = "최소 주문 금액은 0 이상이어야 합니다")
    private BigDecimal minOrderAmount = BigDecimal.ZERO;

    @NotNull(message = "최대 발행 수는 필수입니다")
    @Min(value = 1, message = "최대 발행 수는 1 이상이어야 합니다")
    private Integer maxIssueCount;

    @NotNull(message = "유효 시작일은 필수입니다")
    private LocalDateTime validFrom;

    @NotNull(message = "유효 종료일은 필수입니다")
    private LocalDateTime validTo;

    @NotNull(message = "생성자 ID는 필수입니다")
    private Long createdBy;
}
