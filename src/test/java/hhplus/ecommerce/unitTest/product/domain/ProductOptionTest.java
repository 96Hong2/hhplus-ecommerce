package hhplus.ecommerce.unitTest.product.domain;

import hhplus.ecommerce.common.domain.exception.ProductException;
import hhplus.ecommerce.common.domain.exception.StockException;
import hhplus.ecommerce.product.domain.model.ProductOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static hhplus.ecommerce.unitTest.support.DomainTestFixtures.*;

public class ProductOptionTest {

    @Test
    @DisplayName("상품 옵션을 정상적으로 생성할 수 있다.")
    void createProductOption() {
        // when
        ProductOption option = ProductOption.create(1L, "Large", BigDecimal.valueOf(2000), 100, true);
        initTimestamps(option);

        // then
        assertThat(option.getProductId()).isEqualTo(1L);
        assertThat(option.getOptionName()).isEqualTo("Large");
        assertThat(option.getPriceAdjustment()).isEqualByComparingTo(BigDecimal.valueOf(2000));
        assertThat(option.getStockQuantity()).isEqualTo(100);
        assertThat(option.isExposed()).isTrue();
        assertThat(option.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("상품 옵션 생성 시 productId가 null이면 예외가 발생한다.")
    void createProductOptionWithNullProductId() {
        // when & then
        assertThatThrownBy(() -> ProductOption.create(null, "Large", BigDecimal.valueOf(2000), 100, true))
                .isInstanceOf(ProductException.class)
                .hasMessageContaining("상품 ID는 필수입니다.");
    }

    @Test
    @DisplayName("상품 옵션 생성 시 옵션명이 null이면 예외가 발생한다.")
    void createProductOptionWithNullName() {
        // when & then
        assertThatThrownBy(() -> ProductOption.create(1L, null, BigDecimal.valueOf(2000), 100, true))
                .isInstanceOf(ProductException.class)
                .hasMessageContaining("옵션명은 필수입니다.");
    }

    @Test
    @DisplayName("상품 옵션 생성 시 가격 변동이 0이면 예외가 발생한다.")
    void createProductOptionWithZeroPriceAdjustment() {
        // when - 0원은 허용됨(도메인 정책)
        ProductOption option = ProductOption.create(1L, "Large", BigDecimal.ZERO, 100, true);
        assertThat(option.getPriceAdjustment()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("상품 옵션 생성 시 재고 수량이 음수면 예외가 발생한다.")
    void createProductOptionWithNegativeStock() {
        // when & then
        assertThatThrownBy(() -> ProductOption.create(1L, "Large", BigDecimal.valueOf(2000), -1, true))
                .isInstanceOf(StockException.class);
    }

    @Test
    @DisplayName("재고를 정상적으로 감소시킬 수 있다.")
    void decreaseStock() {
        // given
        ProductOption option = ProductOption.create(1L, "Large", BigDecimal.valueOf(2000), 100, true);

        // when
        option.decreaseStock(30);

        // then
        assertThat(option.getStockQuantity()).isEqualTo(70);
    }

    @Test
    @DisplayName("재고 감소 시 재고가 부족하면 예외가 발생한다.")
    void decreaseStockWithInsufficientQuantity() {
        // given
        ProductOption option = ProductOption.create(1L, "Large", BigDecimal.valueOf(2000), 10, true);

        // when & then
        assertThatThrownBy(() -> option.decreaseStock(20))
                .isInstanceOf(StockException.class);
    }

    @Test
    @DisplayName("재고를 정상적으로 증가시킬 수 있다.")
    void increaseStock() {
        // given
        ProductOption option = ProductOption.create(1L, "Large", BigDecimal.valueOf(2000), 100, true);

        // when
        option.increaseStock(50);

        // then
        assertThat(option.getStockQuantity()).isEqualTo(150);
    }

    @Test
    @DisplayName("재고 증가 시 0 이하의 수량이면 예외가 발생한다.")
    void increaseStockWithInvalidQuantity() {
        // given
        ProductOption option = ProductOption.create(1L, "Large", BigDecimal.valueOf(2000), 100, true);

        // when & then
        assertThatThrownBy(() -> option.increaseStock(0))
                .isInstanceOf(StockException.class);
    }

    @Test
    @DisplayName("재고가 충분한지 확인할 수 있다.")
    void hasEnoughStock() {
        // given
        ProductOption option = ProductOption.create(1L, "Large", BigDecimal.valueOf(2000), 100, true);

        // when & then
        assertThat(option.hasEnoughStock(50)).isTrue();
        assertThat(option.hasEnoughStock(100)).isTrue();
        assertThat(option.hasEnoughStock(101)).isFalse();
    }

    @Test
    @DisplayName("재고가 0이면 품절 상태이다.")
    void isSoldOut() {
        // given
        ProductOption option = ProductOption.create(1L, "Large", BigDecimal.valueOf(2000), 0, true);

        // when & then
        assertThat(option.isSoldOut()).isTrue();

        // 재고 추가
        option.increaseStock(10);
        assertThat(option.isSoldOut()).isFalse();
    }

    @Test
    @DisplayName("구매 가능 여부를 확인할 수 있다.")
    void isAvailableForPurchase() {
        // given - 노출 중이고 재고가 있는 옵션
        ProductOption option = ProductOption.create(1L, "Large", BigDecimal.valueOf(2000), 100, true);

        // when & then
        assertThat(option.isAvailableForPurchase(50)).isTrue();

        // 재고 부족
        assertThat(option.isAvailableForPurchase(101)).isFalse();

        // 품절
        option.setStockQuantity(0);
        assertThat(option.isAvailableForPurchase(1)).isFalse();

        // 비노출
        option.setStockQuantity(100);
        option.hide();
        assertThat(option.isAvailableForPurchase(50)).isFalse();
    }

    @Test
    @DisplayName("상품 옵션 정보를 업데이트할 수 있다.")
    void updateProductOption() {
        // given
        ProductOption option = ProductOption.create(1L, "Large", BigDecimal.valueOf(2000), 100, true);

        // when
        option.update("XLarge", BigDecimal.valueOf(3000), false);

        // then
        assertThat(option.getOptionName()).isEqualTo("XLarge");
        assertThat(option.getPriceAdjustment()).isEqualByComparingTo(BigDecimal.valueOf(3000));
        assertThat(option.isExposed()).isFalse();
    }

    @Test
    @DisplayName("재고 수량을 직접 설정할 수 있다.")
    void setStockQuantity() {
        // given
        ProductOption option = ProductOption.create(1L, "Large", BigDecimal.valueOf(2000), 100, true);

        // when
        option.setStockQuantity(200);

        // then
        assertThat(option.getStockQuantity()).isEqualTo(200);
    }

    @Test
    @DisplayName("상품 옵션을 숨길 수 있다.")
    void hideProductOption() {
        // given
        ProductOption option = ProductOption.create(1L, "Large", BigDecimal.valueOf(2000), 100, true);

        // when
        option.hide();

        // then
        assertThat(option.isExposed()).isFalse();
    }
}
