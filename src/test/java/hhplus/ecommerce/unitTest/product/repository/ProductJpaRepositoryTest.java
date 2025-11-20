package hhplus.ecommerce.unitTest.product.repository;

import hhplus.ecommerce.product.domain.model.Product;
import hhplus.ecommerce.product.domain.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProductJpaRepositoryTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired
    private ProductRepository productJpaRepository;

    @Test
    @DisplayName("JPA: 노출 상품 단건 조회 및 노출여부 반영")
    void findByIdAndExposed() {
        Product p = Product.create("상품A", "전자", "설명", "http://img", BigDecimal.valueOf(10000), true);
        Product saved = productJpaRepository.save(p);

        Optional<Product> found = productJpaRepository.findByIdAndExposed(saved.getProductId());
        assertThat(found).isPresent();

        // 비노출 처리 후 다시 저장
        saved.updateExposure(false);
        productJpaRepository.save(saved);
        Optional<Product> notFound = productJpaRepository.findByIdAndExposed(saved.getProductId());
        assertThat(notFound).isEmpty();
    }

    @Test
    @DisplayName("JPA: 카테고리 및 가격순 조회")
    void categoryAndPriceQueries() {
        productJpaRepository.save(Product.create("P1", "전자", "d", "img", BigDecimal.valueOf(30000), true));
        productJpaRepository.save(Product.create("P2", "전자", "d", "img", BigDecimal.valueOf(10000), true));
        productJpaRepository.save(Product.create("P3", "의류", "d", "img", BigDecimal.valueOf(20000), true));

        Page<Product> electronics = productJpaRepository.findByCategoryOrderByCreatedAtDesc("전자", PageRequest.of(0, 10));
        assertThat(electronics.getTotalElements()).isEqualTo(2);

        Page<Product> asc = productJpaRepository.findAllByPriceAsc(PageRequest.of(0, 10));
        assertThat(asc.getContent()).hasSize(3);
        assertThat(asc.getContent().get(0).getPrice()).isEqualTo(BigDecimal.valueOf(10000));

        Page<Product> desc = productJpaRepository.findAllByPriceDesc(PageRequest.of(0, 10));
        assertThat(desc.getContent().get(0).getPrice()).isEqualTo(BigDecimal.valueOf(30000));

        long exposedCount = productJpaRepository.countAllExposed();
        assertThat(exposedCount).isEqualTo(3);
    }
}
