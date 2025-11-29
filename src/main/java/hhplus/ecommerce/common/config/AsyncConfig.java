package hhplus.ecommerce.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 비동기 처리 설정
 *
 * 기본 스레드풀 크기(8개)를 늘려서 대량 비동기 작업 처리
 */
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(50);  // 코어 스레드 수
        executor.setMaxPoolSize(100);  // 최대 스레드 수
        executor.setQueueCapacity(500); // 큐 크기
        executor.setThreadNamePrefix("async-");
        executor.setWaitForTasksToCompleteOnShutdown(true); // 종료 시 작업 완료 대기
        executor.setAwaitTerminationSeconds(60); // 종료 대기 시간
        executor.initialize();
        return executor;
    }
}
