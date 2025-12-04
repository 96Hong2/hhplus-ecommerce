package hhplus.ecommerce.integrationTest;

import hhplus.ecommerce.common.domain.constants.BusinessConstants;
import hhplus.ecommerce.context.IntegrationTestBase;
import hhplus.ecommerce.order.application.service.PaymentService;
import hhplus.ecommerce.order.application.usecase.CreateOrderUseCase;
import hhplus.ecommerce.order.presentation.dto.request.OrderCreateRequest;
import hhplus.ecommerce.order.presentation.dto.request.OrderItemRequest;
import hhplus.ecommerce.order.presentation.dto.request.PaymentRequest;
import hhplus.ecommerce.order.presentation.dto.response.OrderCreateResponse;
import hhplus.ecommerce.point.application.service.PointService;
import hhplus.ecommerce.product.application.dto.ProductRankingDto;
import hhplus.ecommerce.product.application.service.ProductService;
import hhplus.ecommerce.product.domain.model.PeriodType;
import hhplus.ecommerce.product.domain.model.PopularProduct;
import hhplus.ecommerce.product.domain.model.Product;
import hhplus.ecommerce.product.domain.model.ProductOption;
import hhplus.ecommerce.product.domain.repository.PopularProductRepository;
import hhplus.ecommerce.product.domain.repository.ProductOptionRepository;
import hhplus.ecommerce.product.domain.repository.ProductRepository;
import hhplus.ecommerce.user.domain.model.User;
import hhplus.ecommerce.user.domain.model.UserRole;
import hhplus.ecommerce.user.domain.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 인기상품 조회 통합 테스트
 * Redis 실시간 랭킹 업데이트 및 DB fallback 확인
 */
class PopularProductIntegrationTest extends IntegrationTestBase {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductOptionRepository productOptionRepository;

    @Autowired
    private PopularProductRepository popularProductRepository;

    @Autowired
    private CreateOrderUseCase createOrderUseCase;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PointService pointService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private List<Long> testProductIds;
    private List<Long> testUserIds;

    @BeforeEach
    void setUp() {
        testProductIds = new ArrayList<>();
        testUserIds = new ArrayList<>();

        // Redis 인기상품 키 초기화
        redisTemplate.delete(BusinessConstants.REDIS_TOP_N_DAILY_KEY);
        redisTemplate.delete(BusinessConstants.REDIS_TOP_N_WEEKLY_KEY);
        redisTemplate.delete(BusinessConstants.REDIS_TOP_N_MONTHLY_KEY);
    }

    @AfterEach
    void tearDown() {
        // 테스트 데이터 정리
        if (!testProductIds.isEmpty()) {
            productOptionRepository.deleteAll(
                productOptionRepository.findAll().stream()
                    .filter(po -> testProductIds.contains(po.getProductId()))
                    .toList()
            );
            productRepository.deleteAllById(testProductIds);
        }

        if (!testUserIds.isEmpty()) {
            userRepository.deleteAllById(testUserIds);
        }

        // Redis 정리
        redisTemplate.delete(BusinessConstants.REDIS_TOP_N_DAILY_KEY);
        redisTemplate.delete(BusinessConstants.REDIS_TOP_N_WEEKLY_KEY);
        redisTemplate.delete(BusinessConstants.REDIS_TOP_N_MONTHLY_KEY);
    }

    @Test
    @DisplayName("주문/결제 시 Redis 인기상품 랭킹이 정상적으로 업데이트된다")
    void payOrder_UpdatesRedisRanking() {
        // Given: 테스트 상품 및 사용자 생성
        Product product1 = createTestProduct("인기상품1", BigDecimal.valueOf(10000));
        Product product2 = createTestProduct("인기상품2", BigDecimal.valueOf(20000));
        ProductOption option1 = createTestProductOption(product1.getProductId(), "옵션1", 100);
        ProductOption option2 = createTestProductOption(product2.getProductId(), "옵션2", 100);

        User user = createTestUser("테스트유저1");
        pointService.chargePoint(user.getUserId(), BigDecimal.valueOf(200000), "포인트 충전");

        // When: 주문 생성 및 결제
        OrderItemRequest item1 = new OrderItemRequest();
        item1.setProductOptionId(option1.getProductOptionId());
        item1.setQuantity(5);

        OrderItemRequest item2 = new OrderItemRequest();
        item2.setProductOptionId(option2.getProductOptionId());
        item2.setQuantity(3);

        OrderCreateRequest orderRequest = new OrderCreateRequest();
        orderRequest.setItems(List.of(item1, item2));
        orderRequest.setCouponId(null);

        OrderCreateResponse orderResponse = createOrderUseCase.execute(user.getUserId(), orderRequest);

        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setPaymentMethod("POINT");
        paymentService.payOrder(orderResponse.getOrderId(), paymentRequest);

        // Then: Redis에 판매량이 업데이트되었는지 확인
        Set<ZSetOperations.TypedTuple<String>> dailyRank =
            redisTemplate.opsForZSet().reverseRangeWithScores(BusinessConstants.REDIS_TOP_N_DAILY_KEY, 0, 10);

        assertThat(dailyRank).isNotNull();
        assertThat(dailyRank).hasSize(2);

        // 판매량 확인 (product1: 5개, product2: 3개)
        List<ZSetOperations.TypedTuple<String>> rankList = new ArrayList<>(dailyRank);
        assertThat(rankList.get(0).getValue()).isEqualTo(String.valueOf(product1.getProductId()));
        assertThat(rankList.get(0).getScore()).isEqualTo(5.0);
        assertThat(rankList.get(1).getValue()).isEqualTo(String.valueOf(product2.getProductId()));
        assertThat(rankList.get(1).getScore()).isEqualTo(3.0);
    }

    @Test
    @DisplayName("Redis에 데이터가 있으면 Redis에서 인기상품을 조회한다")
    void getTopProducts_FromRedis_WhenRedisHasData() {
        // Given: Redis에 인기상품 데이터 추가
        Product product1 = createTestProduct("Redis상품1", BigDecimal.valueOf(10000));
        Product product2 = createTestProduct("Redis상품2", BigDecimal.valueOf(20000));
        Product product3 = createTestProduct("Redis상품3", BigDecimal.valueOf(30000));

        redisTemplate.opsForZSet().add(BusinessConstants.REDIS_TOP_N_DAILY_KEY,
            String.valueOf(product1.getProductId()), 200);
        redisTemplate.opsForZSet().add(BusinessConstants.REDIS_TOP_N_DAILY_KEY,
            String.valueOf(product2.getProductId()), 150);
        redisTemplate.opsForZSet().add(BusinessConstants.REDIS_TOP_N_DAILY_KEY,
            String.valueOf(product3.getProductId()), 100);

        // When: 인기상품 조회 (Top 2)
        List<ProductRankingDto> topProducts = productService.getTopProducts(PeriodType.DAILY, 2);

        // Then: Redis에서 조회한 Top 2 결과 반환 (순위대로)
        assertThat(topProducts).hasSize(2);
        assertThat(topProducts.get(0).getProductId()).isEqualTo(product1.getProductId());
        assertThat(topProducts.get(0).getSalesCount()).isEqualTo(200);
        assertThat(topProducts.get(0).getRank()).isEqualTo(1);
        assertThat(topProducts.get(1).getProductId()).isEqualTo(product2.getProductId());
        assertThat(topProducts.get(1).getSalesCount()).isEqualTo(150);
        assertThat(topProducts.get(1).getRank()).isEqualTo(2);
    }

    @Test
    @DisplayName("여러 번 주문 시 Redis 판매량이 누적된다")
    void multipleOrders_AccumulatesSalesInRedis() {
        // Given: 테스트 상품 및 여러 사용자 생성
        Product product = createTestProduct("누적상품", BigDecimal.valueOf(10000));
        ProductOption option = createTestProductOption(product.getProductId(), "옵션", 100);

        User user1 = createTestUser("유저1");
        User user2 = createTestUser("유저2");
        pointService.chargePoint(user1.getUserId(), BigDecimal.valueOf(100000), "충전");
        pointService.chargePoint(user2.getUserId(), BigDecimal.valueOf(100000), "충전");

        // When: 유저1이 3개 주문
        OrderItemRequest item1 = new OrderItemRequest();
        item1.setProductOptionId(option.getProductOptionId());
        item1.setQuantity(3);

        OrderCreateRequest orderRequest1 = new OrderCreateRequest();
        orderRequest1.setItems(List.of(item1));
        orderRequest1.setCouponId(null);

        OrderCreateResponse orderResponse1 = createOrderUseCase.execute(user1.getUserId(), orderRequest1);

        PaymentRequest paymentRequest1 = new PaymentRequest();
        paymentRequest1.setPaymentMethod("POINT");
        paymentService.payOrder(orderResponse1.getOrderId(), paymentRequest1);

        // 유저2가 5개 주문
        OrderItemRequest item2 = new OrderItemRequest();
        item2.setProductOptionId(option.getProductOptionId());
        item2.setQuantity(5);

        OrderCreateRequest orderRequest2 = new OrderCreateRequest();
        orderRequest2.setItems(List.of(item2));
        orderRequest2.setCouponId(null);

        OrderCreateResponse orderResponse2 = createOrderUseCase.execute(user2.getUserId(), orderRequest2);

        PaymentRequest paymentRequest2 = new PaymentRequest();
        paymentRequest2.setPaymentMethod("POINT");
        paymentService.payOrder(orderResponse2.getOrderId(), paymentRequest2);

        // Then: Redis에 누적 판매량(8개) 확인
        Double score = redisTemplate.opsForZSet().score(
            BusinessConstants.REDIS_TOP_N_DAILY_KEY,
            String.valueOf(product.getProductId())
        );

        assertThat(score).isNotNull();
        assertThat(score).isEqualTo(8.0);
    }

    // === 테스트 헬퍼 메소드 ===

    private Product createTestProduct(String name, BigDecimal price) {
        Product product = productService.registerProduct(
            name, "전자제품", "테스트 상품", "test.jpg", price, true
        );
        testProductIds.add(product.getProductId());
        return product;
    }

    private ProductOption createTestProductOption(Long productId, String optionName, int stock) {
        return productService.createProductOption(
            productId, optionName, BigDecimal.ZERO, stock, true
        );
    }

    private User createTestUser(String name) {
        User user = userRepository.save(User.create(name, UserRole.CUSTOMER));
        testUserIds.add(user.getUserId());
        return user;
    }
}
