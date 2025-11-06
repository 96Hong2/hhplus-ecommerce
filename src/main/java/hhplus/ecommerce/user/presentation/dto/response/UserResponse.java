package hhplus.ecommerce.user.presentation.dto.response;

import hhplus.ecommerce.user.domain.model.User;

public record UserResponse(
        Long userId,
        String username,
        String role,
        String createdAt,
        String updatedAt
) {
    // User Entity -> User Response 변환 팩토리 메서드
    public static UserResponse of(User user) {
        return new UserResponse(
                user.getUserId(),
                user.getUsername(),
                user.getRole().getDescription(),
                user.getCreatedAt().toString(),
                user.getUpdatedAt().toString()
        );
    }
}
