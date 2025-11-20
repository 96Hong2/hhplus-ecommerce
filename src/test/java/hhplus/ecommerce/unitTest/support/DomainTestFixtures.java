package hhplus.ecommerce.unitTest.support;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

public final class DomainTestFixtures {

    private DomainTestFixtures() {}

    public static <T> T setId(T target, String idFieldName, Object idValue) {
        setField(target, idFieldName, idValue);
        return target;
    }

    public static <T> T initTimestamps(T target) {
        LocalDateTime now = LocalDateTime.now();
        // createdAt/updatable=false, updatedAt 를 테스트에서 채워 NPE 방지
        trySetField(target, "createdAt", now);
        trySetField(target, "updatedAt", now);
        trySetField(target, "reservedAt", now);
        trySetField(target, "expiresAt", now.plusMinutes(5));
        return target;
    }

    private static void trySetField(Object target, String fieldName, Object value) {
        try {
            setField(target, fieldName, value);
        } catch (RuntimeException ignored) {
            // 존재하지 않는 필드는 무시
        }
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field f = getFieldRecursive(target.getClass(), fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Field getFieldRecursive(Class<?> clazz, String name) throws NoSuchFieldException {
        Class<?> cur = clazz;
        while (cur != null) {
            try {
                return cur.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                cur = cur.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }
}

