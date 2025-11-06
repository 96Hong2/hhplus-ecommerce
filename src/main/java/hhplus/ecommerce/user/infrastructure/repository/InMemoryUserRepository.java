package hhplus.ecommerce.user.infrastructure.repository;

import hhplus.ecommerce.user.domain.model.User;
import hhplus.ecommerce.user.domain.model.UserRole;
import hhplus.ecommerce.user.domain.repository.UserRepository;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryUserRepository implements UserRepository {

    private final Map<Long, User> userStorage = new HashMap<>();
    private final AtomicLong sequence = new AtomicLong(1);

    @Override
    public User save(User user) {
        Long id = user.getUserId();

        if (id == null) {
            id = sequence.getAndIncrement();
            User newUser = new User(
                    id,
                    user.getUsername(),
                    user.getPointBalance(),
                    user.getRole()
            );
            userStorage.put(id, newUser);
            return newUser;
        }

        userStorage.put(id, user);
        return user;
    }

    @Override
    public Optional<User> findById(Long userId) {
        return Optional.ofNullable(userStorage.get(userId));
    }

    @Override
    public List<User> findAllWithPage(int page, int size) {
        int offset = page * size;
        return userStorage.values().stream()
                .skip(offset)
                .limit(size)
                .toList();
    }

    @Override
    public List<User> findAllByRoleWithPage(UserRole role, int page, int size) {
        int offset = page * size;
        return userStorage.values().stream()
                .filter(user -> user.getRole() == role)
                .skip(offset)
                .limit(size)
                .toList();
    }

    @Override
    public int countAll() {
        return userStorage.size();
    }

    @Override
    public int countByRole(UserRole role) {
        return (int) userStorage.values().stream()
                .filter(user -> user.getRole() == role)
                .count();
    }

    @Override
    public boolean existsByUsername(String username) {
        return userStorage.values().stream()
                .anyMatch(user -> user.getUsername().equals(username));
    }
}
