package hhplus.ecommerce.common.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class DistributedLockAOP {

    private static final String LOCK_PREFIX = "lock:";

    private final RedissonClient redissonClient;
    private final LockTransactionalExecutor txExecutor;

    @Around("@annotation(hhplus.ecommerce.common.application.DistributedLock)")
    public Object lock(ProceedingJoinPoint joinPoint) throws Throwable {

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        DistributedLock annotation = method.getAnnotation(DistributedLock.class);

        // 단순 버전: SpEL 안 쓰고, 첫 번째 인자를 키로 사용한다고 가정
        Object firstArg = joinPoint.getArgs()[0]; // 혹은 원하는 인덱스
        String key = LOCK_PREFIX + firstArg;

        RLock lock = redissonClient.getLock(key);
        boolean locked = false;

        try {
            locked = lock.tryLock(
                annotation.waitTime(),
                annotation.leaseTime(),
                annotation.timeUnit()
            );
            if (!locked) {
                throw new RuntimeException("락을 획득하지 못했습니다. key=" + key);
            }

            // 락 잡은 상태에서, 트랜잭션 새로 열고 비즈니스 로직 실행
            return txExecutor.proceedInNewTransaction(joinPoint);

        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                try {
                    lock.unlock();
                } catch (IllegalMonitorStateException e) {
                    log.warn("Redisson lock already unlocked. key={}", key);
                }
            }
        }
    }
}
