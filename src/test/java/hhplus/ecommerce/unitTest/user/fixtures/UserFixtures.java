package hhplus.ecommerce.unitTest.user.fixtures;

import hhplus.ecommerce.user.domain.model.User;
import hhplus.ecommerce.user.domain.model.UserRole;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class UserFixtures {
    private UserFixtures() {}

    public static User userWithTimestamps(Long id, String username, BigDecimal point, UserRole role) {
        User u = new User(id, username, point, role);
        setTimestamp(u, "createdAt", LocalDateTime.now());
        setTimestamp(u, "updatedAt", LocalDateTime.now());
        return u;
    }

    private static void setTimestamp(User user, String fieldName, LocalDateTime value) {
        try {
            Field f = User.class.getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(user, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set timestamp field: " + fieldName, e);
        }
    }
}
