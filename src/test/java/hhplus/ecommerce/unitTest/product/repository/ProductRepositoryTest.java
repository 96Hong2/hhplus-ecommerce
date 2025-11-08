package hhplus.ecommerce.unitTest.product.repository;

import hhplus.ecommerce.common.presentation.response.PageResponse;
import hhplus.ecommerce.product.domain.model.Product;
import hhplus.ecommerce.product.infrastructure.repository.InMemoryProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ProductRepositoryTest {

    private InMemoryProductRepository productRepository;

    @BeforeEach
    void setUp() {
        productRepository = new InMemoryProductRepository();
    }

    @Test
    @DisplayName("상품을 저장할 수 있다")
    void saveProduct() {
        Product product = Product.create(
                "테스트 상품",
                "전자제품",
                "테스트 상품입니다",
                "http://image.url",
                BigDecimal.valueOf(10000),
                true
        );

        Product savedProduct = productRepository.save(product);

        assertThat(savedProduct).isNotNull();
        assertThat(savedProduct.getProductId()).isNotNull();
        assertThat(savedProduct.getProductName()).isEqualTo("테스트 상품");
    }

    @Test
    @DisplayName("ID로 상품을 조회할 수 있다")
    void findById() {
        Product product = Product.create(
                "테스트 상품",
                "전자제품",
                "테스트 상품입니다",
                "http://image.url",
                BigDecimal.valueOf(10000),
                true
        );
        Product savedProduct = productRepository.save(product);

        Optional<Product> foundProduct = productRepository.findById(savedProduct.getProductId());

        assertThat(foundProduct).isPresent();
        assertThat(foundProduct.get().getProductName()).isEqualTo("테스트 상품");
    }

    @Test
    @DisplayName("카테고리로 상품 목록을 조회할 수 있다")
    void findAllByCategoryWithPage() {
        productRepository.save(Product.create("상품1", "전자제품", "설명1", "http://url1", BigDecimal.valueOf(10000), true));
        productRepository.save(Product.create("상품2", "전자제품", "설명2", "http://url2", BigDecimal.valueOf(20000), true));
        productRepository.save(Product.create("상품3", "의류", "설명3", "http://url3", BigDecimal.valueOf(30000), true));

        PageResponse<Product> result = productRepository.findAllByCategoryWithPage(0, 10, "전자제품");

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("가격순으로 상품 목록을 조회할 수 있다")
    void findAllByPriceWithPaging() {
        productRepository.save(Product.create("상품1", "전자제품", "설명1", "http://url1", BigDecimal.valueOf(30000), true));
        productRepository.save(Product.create("상품2", "전자제품", "설명2", "http://url2", BigDecimal.valueOf(10000), true));
        productRepository.save(Product.create("상품3", "의류", "설명3", "http://url3", BigDecimal.valueOf(20000), true));

        PageResponse<Product> result = productRepository.findAllByPriceWithPaging(0, 10, true);

        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getContent().get(0).getPrice()).isEqualTo(BigDecimal.valueOf(10000));
    }

    @Test
    @DisplayName("인기 상품 목록을 조회할 수 있다")
    void findTopN() {
        Product product1 = Product.create("상품1", "전자제품", "설명1", "http://url1", BigDecimal.valueOf(10000), true);
        Product product2 = Product.create("상품2", "전자제품", "설명2", "http://url2", BigDecimal.valueOf(20000), true);

        // claude review : updateSalesCount 메서드가 없으므로 increaseSalesCount 사용
        product1.increaseSalesCount(100L);
        product2.increaseSalesCount(200L);

        productRepository.save(product1);
        productRepository.save(product2);

        List<Product> topProducts = productRepository.findTopN(2, 3);

        assertThat(topProducts).hasSize(2);
        assertThat(topProducts.get(0).getSalesCount()).isEqualTo(200L);
    }
}
