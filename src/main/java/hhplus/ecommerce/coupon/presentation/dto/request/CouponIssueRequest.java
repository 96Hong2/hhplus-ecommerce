package hhplus.ecommerce.coupon.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class CouponIssueRequest {
    @NotNull(message = "사용자 ID는 필수입니다")
    private Long userId;
}
