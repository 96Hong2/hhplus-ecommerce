package hhplus.ecommerce.context;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

@TestConfiguration(proxyBeanMethods = false)
public class TestContainersConfiguration {

    // 싱글톤으로 만들어서 모든 테스트가 동일한 컨테이너 사용하도록 함
    static MySQLContainer<?> mySQLContainer =
            new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("ecommerce_test")
            .withUsername("test")
            .withPassword("test")
            // .withReuse(true) // 컨테이너 재사용으로 테스트 속도 향상 (CI환경에서는 제거)
            .waitingFor(Wait.forHealthcheck()
                .withStartupTimeout(Duration.ofMinutes(5))); // 5분 타임아웃 헬스체크

    @Bean
    @ServiceConnection
    MySQLContainer<?> mySQLContainer() {
        return mySQLContainer;
    }
}
