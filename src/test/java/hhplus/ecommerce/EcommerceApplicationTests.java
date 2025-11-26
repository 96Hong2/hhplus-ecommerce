package hhplus.ecommerce;

import hhplus.ecommerce.context.TestContainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@Import(TestContainersConfiguration.class)
@TestPropertySource(locations = "classpath:application-test.properties")
class EcommerceApplicationTests {

	// Testcontainer의 동적 포트를 Spring Boot 설정에 주입
	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		// Redis 연결 정보 설정 (Redisson과 Spring Data Redis 모두 지원)
		registry.add("spring.data.redis.host", TestContainersConfiguration.redisContainer::getHost);
		registry.add("spring.data.redis.port", () -> TestContainersConfiguration.redisContainer.getMappedPort(6379).toString());
	}

	@Test
	void contextLoads() {
	}

}
