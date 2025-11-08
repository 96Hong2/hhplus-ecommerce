# hhplus-ecommerce

## 아키텍처 선정

### Layered Architecture (계층형 아키텍처)

이 프로젝트는 **Layered Architecture** 패턴을 채택하여 구현되었습니다.

#### 개념
계층형 아키텍처는 애플리케이션을 여러 계층으로 분리하여 각 계층이 독립적인 책임을 가지도록 구성하는 패턴입니다. 각 계층은 상위 계층에서 하위 계층으로만 의존하며, 역방향 의존성은 허용하지 않습니다.

**계층 구조:**
```
Presentation Layer (Controller)
    ↓
Application Layer (Service)
    ↓
Domain Layer (Entity/Model)
    ↓
Infrastructure Layer (Repository)
```

#### 장점
- **명확한 책임 분리**: 각 계층이 명확한 역할을 가져 코드 이해가 쉬움
- **유지보수성**: 특정 계층만 수정하면 되므로 변경 영향도가 제한적
- **테스트 용이성**: 계층별로 독립적인 단위 테스트 작성 가능
- **학습 곡선이 낮음**: Spring Boot의 표준 구조로 널리 알려진 패턴

#### 단점
- **계층 간 데이터 전달 비용**: DTO 변환 등 계층 간 데이터 매핑 작업이 필요
- **비즈니스 로직 분산 가능성**: Service 계층에 비즈니스 로직이 과도하게 집중될 수 있음
- **확장성 제한**: 도메인이 복잡해질수록 Service 계층이 비대해질 수 있음

#### 선택 이유
1. **Spring Boot 표준**: Spring 생태계에서 가장 널리 사용되는 구조로 레퍼런스가 풍부함
2. **팀 협업 효율성**: 구조가 단순하고 직관적이어서 여러 개발자가 협업하기 좋음
3. **프로젝트 규모**: 이커머스 도메인 규모에 적합한 복잡도
4. **빠른 개발**: 초기 개발 속도가 빠르고 Spring Data JPA와 궁합이 좋음

---

### 도메인별 패키지 구조

이 프로젝트는 도메인별로 레이어드 아키텍처를 구성했습니다.

**패키지 구조:**
```
hhplus.ecommerce
├── product (상품/재고)
│   ├── presentation (Controller, DTO)
│   ├── application (Service)
│   ├── domain (Model, Repository Interface)
│   └── infrastructure (Repository 구현체)
├── order (주문)
│   ├── presentation
│   ├── application
│   ├── domain
│   └── infrastructure
├── user (사용자)
├── point (포인트)
├── cart (장바구니)
├── coupon (쿠폰)
└── common (공통 모듈)
```

#### 선택 이유
1. **도메인 응집도 향상**: 관련 기능이 한 곳에 모여 있어 파악이 쉬움
2. **변경 영향도 최소화**: 특정 도메인 수정 시 다른 도메인에 영향을 주지 않음
3. **팀 분업 용이**: 도메인별로 작업을 분담하기 좋은 구조
4. **마이크로서비스 전환 고려**: 향후 도메인별로 서비스를 분리할 때 유리함

#### 장점
- **높은 응집도**: 관련된 코드가 한 패키지에 모여 있음
- **낮은 결합도**: 도메인 간 의존성이 명확하게 드러남
- **확장 가능성**: 새로운 도메인 추가 시 기존 코드 영향 최소화

#### 단점
- **공통 로직 중복 가능성**: 도메인 간 유사한 로직이 중복될 수 있음
- **초기 구조 설계 필요**: 도메인 경계를 잘못 나누면 리팩토링 비용이 큼
- **패키지 깊이 증가**: 계층 + 도메인으로 인해 패키지 depth가 깊어짐

---

## 동시성 제어 전략

이 프로젝트에서는 여러 사용자가 동시에 접근할 수 있는 공유 자원(포인트, 재고, 쿠폰)에 대해 동시성 제어를 적용했습니다.

### 1. 포인트 충전/사용 - ReentrantLock 방식

**구현 위치:** `PointService.java`

#### 선택 방식
- **애플리케이션 레벨 비관적 락 (Pessimistic Lock)**
- `ReentrantLock` + `ConcurrentHashMap` 사용
- 사용자별로 독립적인 락 보유

#### 구현 코드
```java
private final ConcurrentHashMap<Long, ReentrantLock> userLockMap = new ConcurrentHashMap<>();

public PointHistory chargePoint(Long userId, BigDecimal amount, String description) {
    ReentrantLock lock = userLockMap.computeIfAbsent(userId, k -> new ReentrantLock());
    lock.lock();
    try {
        // 포인트 충전 로직
    } finally {
        lock.unlock();
    }
}
```

#### 선택 이유
1. **단일 서버 환경**: 현재는 단일 인스턴스 환경으로 애플리케이션 레벨 락으로 충분
2. **사용자별 독립성**: 사용자 A와 B가 동시에 포인트를 충전해도 서로 영향 없음
3. **빠른 성능**: DB 락보다 가볍고 빠름
4. **재시도 불필요**: 락을 획득할 때까지 대기하므로 비즈니스 로직이 단순함

#### 장점
- 구현이 단순하고 직관적
- 성능이 우수 (메모리 기반)
- 사용자별 락으로 동시성 처리 가능

#### 단점
- **확장성 제한**: 멀티 서버 환경에서는 동작하지 않음
- **서버 재시작 시 락 초기화**: 메모리 기반이므로 서버 재시작 시 락 정보 소실

#### 대안 방식
- **DB 비관적 락 (`SELECT FOR UPDATE`)**: DB 레벨에서 Row Lock을 걸어 안전하게 처리 (멀티 서버 환경에 적합하지만 성능 저하)
- **Redis 분산 락 (Redisson)**: 멀티 서버 환경에서 동시성 제어 가능 (Redis 인프라 필요)
- **낙관적 락 (Optimistic Lock)**: Version 필드를 사용해 충돌 감지 후 재시도 (충돌이 적을 때 유리)

---

### 2. 선착순 쿠폰 발급 - CAS (Compare-And-Swap) 방식

**구현 위치:** `InMemoryUserCouponRepository.java`

#### 선택 방식
- **애플리케이션 레벨 낙관적 락 (Optimistic Lock)**
- `AtomicInteger`의 `compareAndSet()` 메서드 사용
- 무한 루프 + CAS로 발급 수 원자적 증가

#### 구현 코드
```java
private final ConcurrentHashMap<Long, AtomicInteger> issueCountMap = new ConcurrentHashMap<>();

public boolean incrementIssueCountIfAvailable(Long couponId, Integer maxIssueCount) {
    AtomicInteger count = issueCountMap.computeIfAbsent(couponId, k -> new AtomicInteger(0));

    while (true) {
        int current = count.get();

        // 최대 발급 수 초과 체크
        if (current >= maxIssueCount) {
            return false;
        }

        // CAS로 원자적으로 증가
        if (count.compareAndSet(current, current + 1)) {
            return true;
        }
        // 실패 시 재시도
    }
}
```

#### 선택 이유
1. **높은 동시성 환경**: 선착순 쿠폰은 순간적으로 대량 요청이 몰림
2. **Lock-Free 알고리즘**: 락을 걸지 않아 성능이 우수함
3. **원자성 보장**: CAS 연산이 하드웨어 레벨에서 원자성을 보장
4. **재시도 비용 낮음**: 충돌 시 빠르게 재시도 가능

#### 장점
- Lock을 사용하지 않아 성능이 우수
- 데드락 위험 없음
- CPU 캐시 친화적

#### 단점
- **확장성 제한**: 단일 서버 환경에서만 동작
- **재시도 오버헤드**: 충돌이 많으면 재시도가 반복될 수 있음
- **ABA 문제 가능성**: 값이 A→B→A로 변경될 경우 감지 불가 (현재 시나리오에서는 발생하지 않음)

#### 대안 방식
- **Redis 분산 락 + INCR**: Redis의 INCR 명령어로 원자적 증가 (멀티 서버 환경 적합)
- **DB 비관적 락**: Coupon 테이블에 `SELECT FOR UPDATE`로 Row Lock (안전하지만 느림)
- **메시지 큐 (Kafka, RabbitMQ)**: 쿠폰 발급 요청을 큐에 넣고 순차 처리 (대규모 트래픽에 적합)

---

### 3. 재고 관리 - 재고 예약 시스템

**구현 위치:** `StockService.java`, `StockReservation.java`

#### 선택 방식
- **재고 예약 (Reservation) 패턴**
- 주문 생성 시 재고를 즉시 차감하지 않고 15분간 예약
- 결제 완료 시 예약 확정 (실제 차감)
- 결제 미완료 시 예약 자동 해제 (재고 복원)

#### 동작 흐름
```
1. 주문 생성 → 재고 예약 (RESERVED)
   - 실제 재고(physicalStock)는 유지
   - 예약 재고(reservedStock)만 증가

2. 결제 완료 → 재고 확정 (CONFIRMED)
   - 실제 재고(physicalStock) 차감
   - 예약 상태를 CONFIRMED로 변경

3. 결제 미완료 (15분 초과) → 재고 해제 (RELEASED)
   - 예약 상태를 RELEASED로 변경
   - 예약 재고에서 제외 (다른 사용자가 주문 가능)
```

#### 선택 이유
1. **주문-결제 분리 구조**: 주문과 결제가 별도 API로 분리된 시스템에 적합
2. **사용자 경험 개선**: 주문서 작성 중 재고가 소진되는 것을 방지
3. **재고 낭비 방지**: 결제하지 않은 주문이 재고를 영구 점유하지 않도록 함
4. **타임아웃 정책**: 15분 후 자동 해제로 재고 회전율 향상

#### 장점
- 주문과 결제 프로세스를 안전하게 분리 가능
- 타임아웃으로 재고 낭비 방지
- 예약 상태 추적으로 재고 이력 관리 용이

#### 단점
- **동시성 제어 미흡**: 현재 코드에서는 재고 예약 시점의 동시성 제어가 명확하지 않음
- **복잡도 증가**: 예약 상태 관리 로직이 추가됨
- **배치 작업 필요**: 만료된 예약을 주기적으로 정리하는 스케줄러 필요

#### 향후 개선 방안
- **DB 비관적 락**: `ProductOption` 조회 시 `SELECT FOR UPDATE` 적용
- **Redis 분산 락**: 멀티 서버 환경 대비
- **낙관적 락**: `ProductOption`에 Version 필드 추가하여 충돌 감지

#### 대안 방식
- **즉시 차감 방식**: 주문 생성 시 재고 즉시 차감 (취소 시 복원) - 간단하지만 재고 낭비 가능
- **메시지 큐 방식**: 재고 차감 요청을 큐에 넣고 순차 처리 - 대규모 트래픽에 적합하지만 복잡도 높음

---

## 동시성 제어 방식 비교표

| 구분 | 포인트 | 선착순 쿠폰 | 재고 |
|------|--------|-------------|------|
| **방식** | ReentrantLock | AtomicInteger CAS | 재고 예약 시스템 |
| **락 타입** | 비관적 락 | 낙관적 락 | 예약 패턴 (동시성 제어 보완 필요) |
| **레벨** | 애플리케이션 | 애플리케이션 | 애플리케이션 + 비즈니스 로직 |
| **멀티 서버** | 불가 | 불가 | 불가 (향후 DB 락 또는 Redis 락 필요) |
| **성능** | 높음 | 매우 높음 | 중간 |
| **구현 복잡도** | 낮음 | 중간 | 높음 |

---

## 참고 자료
- [Spring Boot 공식 문서](https://spring.io/projects/spring-boot)
- [Java Concurrency in Practice](https://jcip.net/)
- API 명세: `docs/api/api-specification.md`
- 데이터 모델: `docs/api/data-models.md`