package hhplus.ecommerce;

import hhplus.ecommerce.context.TestContainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@Import(TestContainersConfiguration.class)
@TestPropertySource(locations = "classpath:application-test.properties")
class EcommerceApplicationTests {

	@Test
	void contextLoads() {
	}

}
