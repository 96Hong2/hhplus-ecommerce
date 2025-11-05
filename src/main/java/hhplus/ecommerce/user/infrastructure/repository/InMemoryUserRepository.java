package hhplus.ecommerce.user.infrastructure.repository;

import hhplus.ecommerce.user.domain.model.User;
import hhplus.ecommerce.user.domain.repository.UserRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@Repository
public class InMemoryUserRepository implements UserRepository {

    HashMap<Long, User> userStorage = new HashMap<>();

    @Override
    public User save(User user) {
        return userStorage.put(user.getUserId(), user);
    }

    @Override
    public Optional<User> findById(Long userId) {
        return Optional.of(userStorage.get(userId));
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
    public BigDecimal findPointBalanceByUserId(Long userId) {
        return userStorage.get(userId).getPointBalance();
    }
}
