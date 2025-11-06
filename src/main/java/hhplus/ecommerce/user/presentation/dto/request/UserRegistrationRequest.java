package hhplus.ecommerce.user.presentation.dto.request;

import hhplus.ecommerce.user.domain.model.UserRole;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRegistrationRequest {
    private String username;
    private UserRole role;
}
