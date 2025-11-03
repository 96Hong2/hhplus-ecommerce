package hhplus.ecommerce.domain.repository;

import hhplus.ecommerce.domain.model.user.User;
import java.util.Optional;

public interface UserRepository {
    User save(User user);
    Optional<User> findById(Long userId);
    boolean existsByUsername(String username);
}
