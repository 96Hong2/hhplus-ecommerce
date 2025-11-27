package hhplus.ecommerce.common.application;

import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 트랜잭션용 헬퍼 클래스
 */
@Component
public class LockTransactionalExecutor {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Object proceedInNewTransaction(ProceedingJoinPoint joinPoint) throws Throwable {
        return joinPoint.proceed();
    }
}
