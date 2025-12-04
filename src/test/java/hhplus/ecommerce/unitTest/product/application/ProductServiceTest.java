package hhplus.ecommerce.unitTest.product.application;

import hhplus.ecommerce.common.domain.constants.BusinessConstants;
import hhplus.ecommerce.common.domain.exception.ProductException;
import hhplus.ecommerce.product.application.dto.ProductRankingDto;
import hhplus.ecommerce.product.application.service.ProductService;
import hhplus.ecommerce.product.domain.model.PeriodType;
import hhplus.ecommerce.product.domain.model.PopularProduct;
import hhplus.ecommerce.product.domain.model.Product;
import hhplus.ecommerce.product.domain.model.ProductOption;
import hhplus.ecommerce.product.domain.repository.PopularProductRepository;
import hhplus.ecommerce.product.domain.repository.ProductOptionRepository;
import hhplus.ecommerce.product.domain.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private PopularProductRepository popularProductRepository;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

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

    @Test
    @DisplayName("인기 상품을 Redis에서 조회할 수 있다")
    void getTopProductsFromRedis() {
        // given
        PeriodType period = PeriodType.DAILY;
        int limit = 5;

        // productId가 있는 Product 생성
        Product productWithId = Product.create(
            "테스트 상품",
            "전자제품",
            "테스트 상품입니다",
            "http://image.url",
            BigDecimal.valueOf(10000),
            true
        );
        // Reflection으로 productId 설정
        try {
            var field = Product.class.getDeclaredField("productId");
            field.setAccessible(true);
            field.set(productWithId, 1L);
        } catch (Exception e) {
            // ignore
        }

        // Redis 모킹
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

        ZSetOperations.TypedTuple<String> tuple = mock(ZSetOperations.TypedTuple.class);
        when(tuple.getValue()).thenReturn("1");
        when(tuple.getScore()).thenReturn(100.0);

        Set<ZSetOperations.TypedTuple<String>> mockRanking = Set.of(tuple);
        when(zSetOperations.reverseRangeWithScores(BusinessConstants.REDIS_TOP_N_DAILY_KEY, 0, limit - 1))
                .thenReturn(mockRanking);

        when(productRepository.findAllById(anyList())).thenReturn(List.of(productWithId));

        // when
        List<ProductRankingDto> result = productService.getTopProducts(period, limit);

        // then
        assertThat(result).isNotEmpty();
        assertThat(result.get(0).getRank()).isEqualTo(1);
        verify(zSetOperations, times(1)).reverseRangeWithScores(BusinessConstants.REDIS_TOP_N_DAILY_KEY, 0, limit - 1);
    }

    @Test
    @DisplayName("Redis가 비어있을 때 DB에서 인기 상품을 조회할 수 있다")
    void getTopProductsFromDbWhenRedisIsEmpty() {
        // given
        PeriodType period = PeriodType.WEEKLY;
        int limit = 5;

        // productId가 있는 Product 생성
        Product productWithId = Product.create(
            "테스트 상품",
            "전자제품",
            "테스트 상품입니다",
            "http://image.url",
            BigDecimal.valueOf(10000),
            true
        );
        // Reflection으로 productId 설정
        try {
            var field = Product.class.getDeclaredField("productId");
            field.setAccessible(true);
            field.set(productWithId, 1L);
        } catch (Exception e) {
            // ignore
        }

        // Redis 비어있음
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.reverseRangeWithScores(BusinessConstants.REDIS_TOP_N_WEEKLY_KEY, 0, limit - 1))
                .thenReturn(Set.of());

        // DB에서 조회
        PopularProduct popularProduct = PopularProduct.create(1L, 100, LocalDate.now(), PeriodType.WEEKLY, 1);
        when(popularProductRepository.findTopNByPeriodType(period, PageRequest.of(0, limit)))
                .thenReturn(List.of(popularProduct));

        when(productRepository.findAllById(anyList())).thenReturn(List.of(productWithId));

        // when
        List<ProductRankingDto> result = productService.getTopProducts(period, limit);

        // then
        assertThat(result).isNotEmpty();
        assertThat(result.get(0).getRank()).isEqualTo(1);
        verify(popularProductRepository, times(1)).findTopNByPeriodType(period, PageRequest.of(0, limit));
    }

    @Test
    @DisplayName("잘못된 limit 값으로 인기 상품 조회 시 예외가 발생한다")
    void getTopProductsWithInvalidLimit() {
        // given
        PeriodType period = PeriodType.DAILY;
        int invalidLimit = 0;

        // when & then
        assertThatThrownBy(() -> productService.getTopProducts(period, invalidLimit))
                .isInstanceOf(ProductException.class);
    }
}
