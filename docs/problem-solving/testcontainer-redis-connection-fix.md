# Testcontainer Redis 연결 문제 해결

## 문제 상황

통합 테스트 실행 시 Redis 연결 오류 발생:
```
Unable to connect to Redis server: localhost/127.0.0.1:6379
java.net.ConnectException: Connection refused
```

## 원인

**Testcontainer는 동적 포트를 사용하지만, Spring Boot는 정적 포트로 연결 시도**

- Testcontainer: 포트 충돌 방지를 위해 매번 다른 포트 사용 (예: 50123, 50124...)
- Spring Boot: `application-test.properties`의 고정 포트 6379로 연결 시도

### 왜 Testcontainer는 동적 포트를 사용하는가?

1. **포트 충돌 방지**: 로컬에 이미 Redis가 실행 중이어도 충돌 없음
2. **병렬 테스트 지원**: 여러 테스트가 동시에 각자의 Redis 컨테이너 사용 가능
3. **CI 환경 안정성**: 여러 빌드가 동시 실행되어도 격리 보장

## 해결 방법

### 1단계: IntegrationTestBase 클래스 생성

모든 통합 테스트의 공통 설정을 제공하는 Base 클래스:

```java
@SpringBootTest
@Import(TestContainersConfiguration.class)
@TestPropertySource(locations = "classpath:application-test.properties")
public abstract class IntegrationTestBase {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Testcontainer의 동적 Redis 포트를 Spring 설정에 주입
        registry.add("spring.data.redis.host",
            TestContainersConfiguration.redisContainer::getHost);
        registry.add("spring.data.redis.port",
            () -> TestContainersConfiguration.redisContainer.getMappedPort(6379).toString());
    }
}
```

**위치**: `src/test/java/hhplus/ecommerce/context/IntegrationTestBase.java`

### 2단계: 통합 테스트에서 상속

기존 테스트를 Base 클래스 상속으로 변경:

```java
// Before
@SpringBootTest
@Import(TestContainersConfiguration.class)
@TestPropertySource(locations = "classpath:application-test.properties")
class CouponConcurrencyTest {
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // 중복된 설정...
    }
}

// After
class CouponConcurrencyTest extends IntegrationTestBase {
    // @DynamicPropertySource 불필요!
}
```

## 핵심 개념

### @DynamicPropertySource의 역할

실행 순서:
1. Testcontainer 시작 → Redis가 동적 포트(예: 50123)에 바인딩
2. `@DynamicPropertySource` 실행 → Spring에 `spring.data.redis.port=50123` 전달
3. Spring ApplicationContext 로딩 → Redis 클라이언트가 올바른 포트로 연결 ✅

### 고정 포트 vs 동적 포트

| 항목 | 고정 포트 | 동적 포트 |
|------|----------|----------|
| 로컬 Redis 충돌 | ❌ 발생 | ✅ 없음 |
| 병렬 테스트 실행 | ❌ 불가 | ✅ 가능 |
| CI 안정성 | ❌ 불안정 | ✅ 안정 |
| 설정 복잡도 | ✅ 낮음 | ⚠️ 중간 |

## 적용 결과

**수정된 파일**:
- `IntegrationTestBase.java` (생성)
- `EcommerceApplicationTests.java`
- `CouponConcurrencyTest.java`
- `PointConcurrencyTest.java`
- `OrderPaymentIntegrationTest.java`
- `QueryPerformanceTest.java`
- `StockConcurrencyTest.java`

**테스트 결과**: 모든 통합 테스트 통과 ✅

## CI 환경 호환성

`.github/workflows/ci.yml`에 Redis 서비스가 이미 구성되어 있으며, `@DynamicPropertySource`는 Testcontainer와 함께 작동하므로 CI에서도 정상 동작합니다.

## 참고

- 중복 코드 제거로 유지보수성 향상
- 새로운 통합 테스트는 `IntegrationTestBase`만 상속하면 됨
- Testcontainer의 동적 포트 메커니즘은 Docker의 포트 매핑 기능을 활용
