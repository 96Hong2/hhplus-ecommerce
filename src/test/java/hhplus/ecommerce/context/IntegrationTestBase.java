package hhplus.ecommerce.context;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

/**
 * 통합 테스트 Base 클래스
 *
 * Testcontainer를 사용하는 모든 통합 테스트의 공통 설정을 제공합니다.
 * - Redis 동적 포트 설정 (Testcontainer가 할당한 포트를 Spring에 주입)
 * - MySQL 컨테이너 연결 (@ServiceConnection으로 자동 설정)
 *
 * 사용법:
 * <pre>
 * class MyIntegrationTest extends IntegrationTestBase {
 *     // @DynamicPropertySource 불필요
 *     @Test
 *     void test() { ... }
 * }
 * </pre>
 */
@SpringBootTest
@Import(TestContainersConfiguration.class)
@TestPropertySource(locations = "classpath:application-test.properties")
public abstract class IntegrationTestBase {

    /**
     * Testcontainer의 동적 포트를 Spring Boot 설정에 주입
     *
     * Testcontainer는 포트 충돌 방지를 위해 동적 포트를 사용합니다.
     * 이 메서드는 컨테이너가 실제 사용하는 포트를 Spring 설정에 전달합니다. (properties 파일의 고정 포트 설정보다 우선순위가 높음!)
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Redis 연결 정보 설정
        registry.add("spring.data.redis.host", TestContainersConfiguration.redisContainer::getHost);
        registry.add("spring.data.redis.port",
            () -> TestContainersConfiguration.redisContainer.getMappedPort(6379).toString());
    }
}
