package hhplus.ecommerce.common.application;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {

    // 락 키 (ex. #productOptionId)
    String key();

    long waitTime() default 30;
    long leaseTime() default 30;
    TimeUnit timeUnit() default TimeUnit.SECONDS;
}
