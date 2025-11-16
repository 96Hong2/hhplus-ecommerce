package hhplus.ecommerce.unitTest.product.domain;

import hhplus.ecommerce.common.domain.exception.ProductException;
import hhplus.ecommerce.product.domain.model.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static hhplus.ecommerce.unitTest.support.DomainTestFixtures.*;

public class ProductTest {

    @Test
    @DisplayName("상품을 정상적으로 등록할 수 있다.")
    void createProduct() {
        // when
        Product product = Product.create("testProduct", "testCategory", "testDescription",
                "testImageUrl", BigDecimal.valueOf(10000), true);
        setId(product, "productId", 1L);
        initTimestamps(product);

        // then
        assertThat(product.getProductId()).isNotNull();
        assertThat(product.getProductName()).isEqualTo("testProduct");
        assertThat(product.getCategory()).isEqualTo("testCategory");
        assertThat(product.getDescription()).isEqualTo("testDescription");
        assertThat(product.getImageUrl()).isEqualTo("testImageUrl");
        assertThat(product.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(10000));
        assertThat(product.getSalesCount()).isZero();
        assertThat(product.isExposed()).isTrue();
        assertThat(product.isDeleted()).isFalse();
        assertThat(product.getCreatedAt()).isNotNull();
        assertThat(product.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("상품 등록 시 필수 파라미터가 없으면 적절한 예외가 발생한다.")
    public void createProductWithNullParams() {
        assertThatThrownBy(() -> Product.create(null, "testCategory", null, null, BigDecimal.valueOf(10000), false))
                .isInstanceOf(ProductException.class)
                .hasMessageContaining("상품명은 필수입니다.");

        assertThatThrownBy(() -> Product.create("testProductName", "", null, null, BigDecimal.valueOf(10000), false))
                .isInstanceOf(ProductException.class)
                .hasMessageContaining("카테고리는 필수입니다.");
    }

    @Test
    @DisplayName("상품 등록 시 가격이 음수면 예외가 발생한다.")
    public void createProductWithNegativePrice() {
        assertThatThrownBy(() -> Product.create("testProduct", "testCategory", null, null, BigDecimal.valueOf(-1000), false))
                .isInstanceOf(ProductException.class)
                .hasMessageContaining("상품 가격은 0원 이상이어야 합니다.");
    }

    @Test
    @DisplayName("상품 업데이트 시 정상적으로 업데이트된다.")
    public void updateProduct() {
        // given
        Product product = Product.create("testProduct", "testCategory", "testDescription",
                "testImageUrl", BigDecimal.valueOf(10000), true);

        // when
        product.update("newTestProduct", "newTestCategory", "newTestDescription",
                "newTestImageUrl", BigDecimal.valueOf(20000), 100, false);

        // then
        assertThat(product.getProductName()).isEqualTo("newTestProduct");
        assertThat(product.getCategory()).isEqualTo("newTestCategory");
        assertThat(product.getDescription()).isEqualTo("newTestDescription");
        assertThat(product.getImageUrl()).isEqualTo("newTestImageUrl");
        assertThat(product.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(20000));
        assertThat(product.getSalesCount()).isEqualTo(100);
        assertThat(product.isExposed()).isFalse();
    }

    @Test
    @DisplayName("상품 삭제 시 논리적 삭제되고 노출 플래그도 false로 변경된다.")
    public void deleteProduct() {
        // given
        Product product = Product.create("testProduct", "testCategory", "testDescription",
                "testImageUrl", BigDecimal.valueOf(10000), true);

        // when
        product.delete();

        // then
        assertThat(product.isDeleted()).isTrue();
        assertThat(product.isExposed()).isFalse();
    }

    @Test
    @DisplayName("판매량 증가 시 정상적으로 증가한다.")
    public void increaseSalesCount() {
        // given
        Product product = Product.create("testProduct", "testCategory", "testDescription",
                "testImageUrl", BigDecimal.valueOf(10000), true);

        // when
        product.increaseSalesCount(10);

        // then
        assertThat(product.getSalesCount()).isEqualTo(10);
    }

    @Test
    @DisplayName("판매량 증가 시 0 이하의 수량이면 예외가 발생한다.")
    public void increaseSalesCountWithInvalidQuantity() {
        // given
        Product product = Product.create("testProduct", "testCategory", "testDescription",
                "testImageUrl", BigDecimal.valueOf(10000), true);

        // when & then
        assertThatThrownBy(() -> product.increaseSalesCount(0))
                .isInstanceOf(ProductException.class)
                .hasMessageContaining("증가 수량은 0보다 커야 합니다.");
    }

    @Test
    @DisplayName("판매량 감소 시 정상적으로 감소한다.")
    public void decreaseSalesCount() {
        // given
        Product product = Product.create("testProduct", "testCategory", "testDescription",
                "testImageUrl", BigDecimal.valueOf(10000), true);
        product.increaseSalesCount(100);

        // when
        product.decreaseSalesCount(30);

        // then
        assertThat(product.getSalesCount()).isEqualTo(70);
    }

    @Test
    @DisplayName("판매량 감소 시 현재 판매량보다 큰 수량이면 예외가 발생한다.")
    public void decreaseSalesCountWithInsufficientQuantity() {
        // given
        Product product = Product.create("testProduct", "testCategory", "testDescription",
                "testImageUrl", BigDecimal.valueOf(10000), true);
        product.increaseSalesCount(10);

        // when & then
        assertThatThrownBy(() -> product.decreaseSalesCount(20))
                .isInstanceOf(ProductException.class)
                .hasMessageContaining("판매량이 감소 수량보다 적습니다.");
    }

    @Test
    @DisplayName("노출 가능한 상품인지 확인할 수 있다.")
    public void canBeDisplayed() {
        // given
        Product product = Product.create("testProduct", "testCategory", "testDescription",
                "testImageUrl", BigDecimal.valueOf(10000), true);

        // when & then
        assertThat(product.canBeDisplayed()).isTrue();

        // 삭제된 경우
        product.delete();
        assertThat(product.canBeDisplayed()).isFalse();
    }

    @Test
    @DisplayName("노출 여부를 변경할 수 있다.")
    public void updateExposure() {
        // given
        Product product = Product.create("testProduct", "testCategory", "testDescription",
                "testImageUrl", BigDecimal.valueOf(10000), true);

        // when
        product.updateExposure(false);

        // then
        assertThat(product.isExposed()).isFalse();
    }
}
