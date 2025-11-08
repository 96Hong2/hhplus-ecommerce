package hhplus.ecommerce.unitTest.product.application;

import hhplus.ecommerce.common.domain.exception.ProductException;
import hhplus.ecommerce.product.application.service.ProductService;
import hhplus.ecommerce.product.domain.model.Product;
import hhplus.ecommerce.product.domain.model.ProductOption;
import hhplus.ecommerce.product.domain.repository.ProductOptionRepository;
import hhplus.ecommerce.product.domain.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductOptionRepository productOptionRepository;

    @InjectMocks
    private ProductService productService;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        testProduct = Product.create(
                "테스트 상품",
                "전자제품",
                "테스트 상품입니다",
                "http://image.url",
                BigDecimal.valueOf(10000),
                true
        );
    }

    @Test
    @DisplayName("상품을 정상적으로 등록할 수 있다")
    void registerProduct() {
        // given
        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Product result = productService.registerProduct(
                "테스트 상품",
                "전자제품",
                "테스트 상품입니다",
                "http://image.url",
                BigDecimal.valueOf(10000),
                true
        );

        // then
        assertThat(result).isNotNull();
        assertThat(result.getProductName()).isEqualTo("테스트 상품");
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    @DisplayName("상품 상세를 조회할 수 있다")
    void getProductDetail() {
        // given
        Long productId = 1L;
        List<ProductOption> options = List.of(
                ProductOption.create(productId, "옵션1", BigDecimal.valueOf(1000), 10, true)
        );

        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(productOptionRepository.findAllByProductId(productId)).thenReturn(options);

        // when
        var result = productService.getProductDetail(productId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getProductName()).isEqualTo("테스트 상품");
        assertThat(result.getProductOptions()).hasSize(1);
        verify(productRepository, times(1)).findById(productId);
        verify(productOptionRepository, times(1)).findAllByProductId(productId);
    }

    @Test
    @DisplayName("존재하지 않는 상품 조회 시 예외가 발생한다")
    void getProductDetailNotFound() {
        // given
        Long productId = 999L;
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> productService.getProductDetail(productId))
                .isInstanceOf(ProductException.class);

        verify(productRepository, times(1)).findById(productId);
    }

    @Test
    @DisplayName("상품 옵션을 정상적으로 등록할 수 있다")
    void createProductOption() {
        // given
        Long productId = 1L;
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(productOptionRepository.save(any(ProductOption.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        ProductOption result = productService.createProductOption(
                productId,
                "옵션1",
                BigDecimal.valueOf(1000),
                10,
                true
        );

        // then
        assertThat(result).isNotNull();
        assertThat(result.getOptionName()).isEqualTo("옵션1");
        verify(productRepository, times(1)).findById(productId);
        verify(productOptionRepository, times(1)).save(any(ProductOption.class));
    }

    @Test
    @DisplayName("존재하지 않는 상품에 옵션 등록 시 예외가 발생한다")
    void createProductOptionForNonExistentProduct() {
        // given
        Long productId = 999L;
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> productService.createProductOption(
                productId,
                "옵션1",
                BigDecimal.valueOf(1000),
                10,
                true
        )).isInstanceOf(ProductException.class);

        verify(productOptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("상품 옵션을 수정할 수 있다")
    void updateProductOption() {
        // given
        Long productOptionId = 1L;
        ProductOption productOption = ProductOption.create(1L, "기존옵션", BigDecimal.valueOf(1000), 10, true);

        when(productOptionRepository.findById(productOptionId)).thenReturn(Optional.of(productOption));
        when(productOptionRepository.save(any(ProductOption.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        ProductOption result = productService.updateProductOption(
                productOptionId,
                "변경된 옵션",
                BigDecimal.valueOf(2000),
                true
        );

        // then
        assertThat(result).isNotNull();
        verify(productOptionRepository, times(1)).findById(productOptionId);
        verify(productOptionRepository, times(1)).save(any(ProductOption.class));
    }

    @Test
    @DisplayName("상품 옵션을 삭제(비노출 처리)할 수 있다")
    void deleteProductOption() {
        // given
        Long productOptionId = 1L;
        ProductOption productOption = ProductOption.create(1L, "옵션", BigDecimal.valueOf(1000), 10, true);

        when(productOptionRepository.findById(productOptionId)).thenReturn(Optional.of(productOption));
        when(productOptionRepository.save(any(ProductOption.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        productService.deleteProductOption(productOptionId);

        // then
        verify(productOptionRepository, times(1)).findById(productOptionId);
        verify(productOptionRepository, times(1)).save(any(ProductOption.class));
    }

    @Test
    @DisplayName("상품 옵션 목록을 조회할 수 있다")
    void getProductOptions() {
        // given
        Long productId = 1L;
        List<ProductOption> expectedOptions = List.of(
                ProductOption.create(productId, "옵션1", BigDecimal.valueOf(1000), 10, true),
                ProductOption.create(productId, "옵션2", BigDecimal.valueOf(2000), 5, true)
        );

        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(productOptionRepository.findAllByProductId(productId)).thenReturn(expectedOptions);

        // when
        List<ProductOption> result = productService.getProductOptions(productId);

        // then
        assertThat(result).hasSize(2);
        verify(productRepository, times(1)).findById(productId);
        verify(productOptionRepository, times(1)).findAllByProductId(productId);
    }
}
