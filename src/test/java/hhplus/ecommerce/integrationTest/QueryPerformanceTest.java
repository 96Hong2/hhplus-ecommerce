package hhplus.ecommerce.integrationTest;

import hhplus.ecommerce.context.IntegrationTestBase;
import hhplus.ecommerce.order.domain.model.Order;
import hhplus.ecommerce.order.domain.model.OrderStatus;
import hhplus.ecommerce.order.domain.repository.OrderRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 쿼리 성능 테스트
 *
 * 목적:
 * 1. EXPLAIN ANALYZE를 통한 실행계획 분석
 * 2. 인덱스 적용 전후 성능 비교
 * 3. N+1 문제 확인 및 개선 효과 검증
 */
@Transactional
class QueryPerformanceTest extends IntegrationTestBase {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private EntityManager em;

    private Statistics statistics;

    @BeforeEach
    void setUp() {
        // Hibernate 통계 초기화
        SessionFactory sessionFactory = em.getEntityManagerFactory()
                .unwrap(SessionFactory.class);
        statistics = sessionFactory.getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
    }

    @Test
    @DisplayName("실행계획 분석 - 만료된 주문 조회")
    void analyzeExpiredOrdersQuery() {
        // Given: 테스트 데이터 생성
        createTestOrders(100);
        em.flush();
        em.clear();

        // When: EXPLAIN 실행
        String explainSql = """
            EXPLAIN
            SELECT * FROM orders
            WHERE order_status = 'PENDING'
              AND expires_at < NOW()
            """;

        List<Object[]> results = em.createNativeQuery(explainSql).getResultList();

        // Then: 실행계획 출력
        System.out.println("\n========== 실행계획 분석 ==========");
        results.forEach(row -> {
            System.out.println("ID: " + row[0]);
            System.out.println("Select Type: " + row[1]);
            System.out.println("Table: " + row[2]);
            System.out.println("Type: " + row[3] + " (ALL=Full Scan, range=Index Range Scan)");
            System.out.println("Possible Keys: " + row[4]);
            System.out.println("Key: " + row[5] + " (사용된 인덱스)");
            System.out.println("Key Length: " + row[6]);
            System.out.println("Ref: " + row[7]);
            System.out.println("Rows: " + row[8] + " (예상 스캔 행 수)");
            System.out.println("Extra: " + row[9]);
            System.out.println("=====================================\n");
        });

        // 실행계획 검증
        Object[] firstRow = results.get(0);
        String scanType = (String) firstRow[3];

        // ALL(Full Table Scan)이면 경고
        if ("ALL".equals(scanType)) {
            System.out.println("⚠️ 경고: Full Table Scan 발생! 인덱스 추가 필요");
        } else {
            System.out.println("✅ 인덱스 사용 중: " + scanType);
        }
    }

    @Test
    @DisplayName("인덱스 적용 전후 성능 비교")
    void comparePerformanceBeforeAfterIndex() {
        // Given: 대량 테스트 데이터 생성 (1000건)
        createTestOrders(1000);
        em.flush();
        em.clear();

        // === 인덱스 적용 전 ===
        long startTime = System.nanoTime();
        List<Order> orders1 = orderRepository.findExpiredOrders(LocalDateTime.now());
        long duration1 = (System.nanoTime() - startTime) / 1_000_000; // ms 변환

        System.out.println("\n========== 성능 비교 ==========");
        System.out.println("인덱스 적용 전: " + duration1 + "ms");
        System.out.println("조회 결과 수: " + orders1.size());

        // When: 인덱스 생성 (이미 있다면 무시)
        try {
            em.createNativeQuery(
                "CREATE INDEX IF NOT EXISTS idx_status_expires " +
                "ON orders(order_status, expires_at)"
            ).executeUpdate();
            em.flush();
            em.clear();
        } catch (Exception e) {
            System.out.println("인덱스가 이미 존재하거나 생성 실패: " + e.getMessage());
        }

        // === 인덱스 적용 후 ===
        startTime = System.nanoTime();
        List<Order> orders2 = orderRepository.findExpiredOrders(LocalDateTime.now());
        long duration2 = (System.nanoTime() - startTime) / 1_000_000;

        System.out.println("인덱스 적용 후: " + duration2 + "ms");
        System.out.println("조회 결과 수: " + orders2.size());

        if (duration1 > 0) {
            double improvement = ((duration1 - duration2) * 100.0 / duration1);
            System.out.println("성능 개선율: " + String.format("%.1f%%", improvement));
        }
        System.out.println("===============================\n");

        // Then: 결과 검증
        assertThat(orders2).hasSameSizeAs(orders1);
    }

    @Test
    @DisplayName("N+1 문제 확인 - 사용자별 주문 조회")
    void checkNPlusOneProblem() {
        // Given: 테스트 데이터 생성
        Long userId = 1L;
        createTestOrdersForUser(userId, 10);
        em.flush();
        em.clear();

        statistics.clear();

        // When: 주문 조회
        List<Order> orders = orderRepository.findByUserId(userId);

        // Then: 통계 출력
        System.out.println("\n========== Hibernate 통계 ==========");
        System.out.println("실행된 쿼리 수: " + statistics.getQueryExecutionCount());
        System.out.println("조회된 엔티티 수: " + statistics.getEntityFetchCount());
        System.out.println("최대 쿼리 실행 시간: " + statistics.getQueryExecutionMaxTime() + "ms");

        // 쿼리별 상세 통계
        String[] queries = statistics.getQueries();
        for (String query : queries) {
            org.hibernate.stat.QueryStatistics queryStats = statistics.getQueryStatistics(query);
            System.out.println("\n쿼리: " + query);
            System.out.println("  실행 횟수: " + queryStats.getExecutionCount());
            System.out.println("  평균 실행 시간: " + queryStats.getExecutionAvgTime() + "ms");
        }
        System.out.println("=====================================\n");

        // N+1 문제 확인 (쿼리 수가 1개면 OK, 여러 개면 N+1 의심)
        long queryCount = statistics.getQueryExecutionCount();
        if (queryCount > 2) {
            System.out.println("⚠️ 경고: N+1 문제 의심 (쿼리 수: " + queryCount + ")");
            System.out.println("💡 해결 방안: Fetch Join 또는 @BatchSize 적용");
        } else {
            System.out.println("✅ N+1 문제 없음");
        }

        assertThat(orders).hasSize(10);
    }

    @Test
    @DisplayName("슬로우 쿼리 로깅 확인")
    void checkSlowQueryLogging() {
        // Given: 테스트 데이터 생성
        createTestOrders(100);
        em.flush();
        em.clear();

        System.out.println("\n========== 슬로우 쿼리 테스트 ==========");
        System.out.println("application-test.properties 설정:");
        System.out.println("- LOG_QUERIES_SLOWER_THAN_MS=50");
        System.out.println("50ms 이상 걸리는 쿼리는 로그에 자동 출력됩니다.");
        System.out.println("========================================\n");

        // When: 복잡한 쿼리 실행
        long startTime = System.nanoTime();
        List<Order> orders = orderRepository.findExpiredOrders(LocalDateTime.now());
        long duration = (System.nanoTime() - startTime) / 1_000_000;

        System.out.println("쿼리 실행 시간: " + duration + "ms");

        if (duration > 50) {
            System.out.println("⚠️ 슬로우 쿼리 발생! 로그를 확인하세요.");
        } else {
            System.out.println("✅ 정상 범위 내 실행 시간");
        }

        assertThat(orders).isNotNull();
    }

    // === 헬퍼 메서드 ===

    private void createTestOrders(int count) {
        for (int i = 0; i < count; i++) {
            String orderNumber = "ORD" + System.currentTimeMillis() + i;
            Long userId = (long) (i % 10 + 1); // 1~10번 사용자

            // 50%는 만료된 주문
            LocalDateTime expiresAt = i % 2 == 0
                ? LocalDateTime.now().minusHours(1)  // 만료됨
                : LocalDateTime.now().plusHours(1);  // 유효함

            Order order = new Order(
                null,
                orderNumber,
                userId,
                BigDecimal.valueOf(10000),
                BigDecimal.ZERO,
                BigDecimal.valueOf(10000),
                null,
                null,
                OrderStatus.PENDING,
                LocalDateTime.now(),
                LocalDateTime.now(),
                expiresAt
            );

            orderRepository.save(order);
        }
    }

    private void createTestOrdersForUser(Long userId, int count) {
        for (int i = 0; i < count; i++) {
            String orderNumber = "ORD" + System.currentTimeMillis() + i;

            Order order = new Order(
                null,
                orderNumber,
                userId,
                BigDecimal.valueOf(10000),
                BigDecimal.ZERO,
                BigDecimal.valueOf(10000),
                null,
                null,
                OrderStatus.PENDING,
                LocalDateTime.now(),
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(1)
            );

            orderRepository.save(order);
        }
    }
}
