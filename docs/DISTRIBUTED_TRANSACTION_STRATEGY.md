# 도메인별 분리 시 트랜잭션 처리의 한계와 대응 방안

## 1. 문제 정의

### 모놀리식 vs 도메인별 분리

```
[모놀리식]
Single Application + Single DB
→ @Transactional로 ACID 보장 ✅

[도메인별 분리]
[Order Service] → [Order DB]
[Product Service] → [Product DB]
[User Service] → [User DB]
→ 단일 트랜잭션 불가 ❌
```

**핵심 문제**:
- 분산 환경에서는 ACID 트랜잭션 불가능
- 네트워크 장애로 인한 부분 실패
- 데이터 정합성 보장 어려움

---

## 2. 현재 방식의 한계 (Spring Events 기반)

### 2.1 현재 구조

```java
// Order Service
@Transactional
public OrderCreateResponse createOrder() {
    Order order = orderRepository.save(order);
    eventPublisher.publish(new OrderCreatedEvent(order));
    return response;
}

// Integration Handler
@TransactionalEventListener(phase = AFTER_COMMIT)
public void handleOrderCreated(OrderCreatedEvent event) {
    externalIntegrationService.sendOrderToERP(order);
}
```

### 2.2 Spring Events 방식의 6가지 한계

#### 문제 1: 단일 JVM 의존성
```
[Order Service - JVM 1]
    ↓ ApplicationEventPublisher (같은 JVM 내)
[OrderIntegrationEventHandler - JVM 1]
```
- ❌ 서비스 간 물리적 분리 불가능
- ❌ 독립 배포 불가
- ❌ 장애 격리 불가

#### 문제 2: 이벤트 유실 가능성
```
1. 주문 생성 트랜잭션 커밋 완료 ✅
2. OrderCreatedEvent 발행 ✅
3. OrderIntegrationEventHandler 실행 중...
4. ⚠️ 애플리케이션 재시작 / 장애 발생
5. ❌ 이벤트 손실 → 데이터 불일치
```
- ❌ 이벤트가 메모리에만 존재
- ❌ 재시도 메커니즘 없음

#### 문제 3: 순서 보장 불가
```
[주문 A 생성] → OrderCreatedEvent(A)
[주문 B 생성] → OrderCreatedEvent(B)
처리 순서: Event(B) → Event(A) (순서 뒤바뀜 가능)
```

#### 문제 4: 백프레셔 부재
```
초당 1000개 주문 생성
외부 ERP 처리 속도: 초당 100개
→ ⚠️ 메모리 부족, 애플리케이션 다운
```

#### 문제 5: 분산 추적 어려움
- 어느 단계에서 실패했는지 추적 곤란
- 이벤트 흐름 가시성 부족

#### 문제 6: 멀티 인스턴스 환경의 한계
```
[Instance 1] → Event 발행 → [Handler in Instance 1]
[Instance 2] → Event 발행 → [Handler in Instance 2]
❌ 인스턴스 간 이벤트 공유 불가
```

---

## 3. 대응 방안 1단계: Outbox 패턴

### 구조
```
[Order Service Transaction]
  1. Order 저장
  2. Outbox 테이블에 이벤트 저장 (같은 트랜잭션)

[Outbox Relay (Polling)]
  3. Outbox 테이블 읽기
  4. ApplicationEventPublisher 발행
  5. Outbox 처리 완료 표시
```

### 구현
```java
@Transactional
public OrderCreateResponse createOrder() {
    Order order = orderRepository.save(order);

    OutboxEvent outboxEvent = new OutboxEvent(
        "OrderCreatedEvent",
        toJson(new OrderCreatedEvent(order))
    );
    outboxRepository.save(outboxEvent);

    return response;
}
```

```sql
CREATE TABLE outbox_events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_type VARCHAR(100) NOT NULL,
    payload JSON NOT NULL,
    created_at TIMESTAMP NOT NULL,
    processed BOOLEAN DEFAULT FALSE
);
```

### 효과
- ✅ 이벤트 영속화 (재시작 후에도 처리 가능)
- ✅ 트랜잭션과 이벤트 발행의 원자성 보장
- ❌ 여전히 단일 JVM 내 (서비스 분리 불가)

---

## 4. 대응 방안 2단계: 메시지 큐 도입 (Kafka) ⭐

### 4.1 Spring Events vs Kafka 비교

| 항목 | Spring Events | Kafka |
|------|--------------|-------|
| **영속성** | ❌ 메모리 | ✅ 디스크 |
| **서비스 분리** | ❌ 단일 JVM | ✅ 완전 독립 |
| **장애 복구** | ❌ 이벤트 손실 | ✅ 재시도 자동화 |
| **순서 보장** | ❌ 불가능 | ✅ Partition Key 기반 |
| **확장성** | ❌ 단일 인스턴스 | ✅ 멀티 컨슈머 그룹 |
| **백프레셔** | ❌ 없음 | ✅ Consumer Lag 조절 |

### 4.2 Kafka 기반 아키텍처

```
[Order Service]
    ↓ 1. Order + Outbox 저장 (트랜잭션)
[Outbox Relay]
    ↓ 2. Kafka Produce
[Kafka Broker]
    ↓ Topic: order-created
[Product Service - Consumer Group A]
    ↓ 3. 재고 차감
[User Service - Consumer Group B]
    ↓ 4. 포인트 차감 (병렬 처리)
```

### 4.3 핵심 구현

#### Order Service (Producer)
```java
@Scheduled(fixedDelay = 1000)
public void relayEvents() {
    List<OutboxEvent> unprocessed = outboxRepository.findByProcessedFalse();

    for (OutboxEvent event : unprocessed) {
        kafkaTemplate.send(event.getEventType(), event.getPayload());
        event.markProcessed();
        outboxRepository.save(event);
    }
}
```

#### Product Service (Consumer)
```java
@KafkaListener(topics = "order-created", groupId = "product-service")
public void handleOrderCreated(OrderCreatedEvent event) {
    try {
        stockService.reduceStock(event.getOrderItems());
        kafkaTemplate.send("stock-reduced", new StockReducedEvent(...));
    } catch (Exception e) {
        kafkaTemplate.send("stock-reduction-failed", new StockReductionFailedEvent(...));
    }
}
```

#### DLQ 처리
```java
@RetryableTopic(
    attempts = "3",
    backoff = @Backoff(delay = 1000, multiplier = 2.0),
    dltTopicSuffix = "-dlt"
)
@KafkaListener(topics = "order-created")
public void handleOrderCreated(OrderCreatedEvent event) {
    // 처리 로직
}

@DltHandler
public void handleDlt(OrderCreatedEvent event, Exception e) {
    log.error("DLT 도착: OrderId={}, 수동 처리 필요", event.getOrderId());
    alertService.notifyAdmin(event, e);
}
```

### 4.4 Kafka 도입의 핵심 이점

#### 1. At-Least-Once 전달 보장
```
[Producer] → [Kafka - Replication] → [Consumer]
Consumer 장애 시: Offset 커밋 전 재시도 → 메시지 손실 방지
```

#### 2. 순서 보장
```java
// Partition Key로 userId 사용
kafkaTemplate.send("order-created", userId.toString(), event);

// 같은 userId의 주문은 같은 파티션 → 순서 보장 ✅
```

#### 3. 서비스 독립성
```
Before: [Order Service (단일 JVM)]
         ├─ Order Logic
         ├─ Integration Handler
         └─ Compensation Handler

After:  [Order Service] → [Kafka] → [Integration Service]
                                  → [Notification Service]
                                  → [Analytics Service]
✅ 각 서비스 독립 배포 및 스케일링
```

---

## 5. 대응 방안 3단계: 분산 추적 및 모니터링

### 5.1 Trace ID 기반 추적

```java
// 이벤트 발행 시 Trace ID 포함
String traceId = UUID.randomUUID().toString();
OrderCreatedEvent event = new OrderCreatedEvent(orderId, traceId);
kafkaTemplate.send("order-created", event);

// 이벤트 수신 시 Trace ID 복원
@KafkaListener(topics = "order-created")
public void handleOrderCreated(OrderCreatedEvent event) {
    MDC.put("traceId", event.getTraceId());
    log.info("재고 차감 시작 - OrderId: {}, TraceId: {}",
             event.getOrderId(), event.getTraceId());
}
```

### 5.2 Saga 상태 관리

```sql
CREATE TABLE saga_instances (
    saga_id VARCHAR(36) PRIMARY KEY,
    aggregate_id BIGINT,  -- 주문 ID
    current_step VARCHAR(50),
    status VARCHAR(20),  -- IN_PROGRESS, COMPENSATING, COMPLETED, FAILED
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

### 5.3 모니터링 지표

**Kafka**:
- Consumer Lag (Lag > 1000: 스케일링 필요)
- DLQ 유입률 (비정상 메시지 비율)

**Saga**:
- Saga 완료율 (COMPLETED / TOTAL)
- 보상 트랜잭션 비율 (COMPENSATED / TOTAL)

---

## 6. 실무 적용 핵심 사항

### 멱등성 보장
```java
@KafkaListener(topics = "order-created")
public void handleOrderCreated(OrderCreatedEvent event) {
    // 중복 처리 방지
    if (processedEventRepository.existsByEventId(event.getEventId())) {
        return;
    }

    stockService.reduceStock(event.getOrderItems());
    processedEventRepository.save(new ProcessedEvent(event.getEventId()));
}
```

### 타임아웃 처리
```java
@KafkaListener(topics = "order-created")
public void handleOrderCreated(OrderCreatedEvent event) {
    if (event.isExpired()) {
        compensate(event);
        return;
    }
    // 정상 처리
}
```

---

## 7. 단계별 마이그레이션 로드맵

### Phase 1: Outbox 패턴 (현재 → 1개월)
```
목표: 이벤트 유실 방지
작업: outbox_events 테이블, OutboxRelay 구현
효과: ✅ 이벤트 영속화, ⚠️ 여전히 단일 JVM
```

### Phase 2: Kafka 도입 (1-2개월) ⭐
```
목표: 서비스 간 물리적 분리
작업: Kafka 클러스터 구축, Integration Service 분리
효과: ✅ 서비스 독립성, ✅ At-Least-Once 보장
```

### Phase 3: 모니터링 및 DLQ (2-3개월)
```
목표: 운영 안정성 확보
작업: Saga 상태 관리, DLQ Handler, 분산 추적
```

### Phase 4: 완전한 MSA (3-6개월)
```
목표: 도메인별 독립 DB + 독립 배포
작업: Product/User Service 분리, API Gateway
```

---

## 8. 결론: 도메인별 분리 시 트랜잭션 처리의 한계와 대응 방안

### 8.1 근본적 한계

1. **ACID 트랜잭션 불가능**
   - 여러 DB에 걸친 원자성 보장 불가
   - 부분 실패 필연적 발생

2. **네트워크 불확실성**
   - 서비스 간 통신 실패 가능
   - 타임아웃, 재시도로 인한 중복 처리 위험

3. **일관성 유지의 어려움**
   - 강한 일관성 → 최종 일관성으로 패러다임 전환
   - 중간 상태 노출 불가피

### 8.2 현재 방식(Spring Events)의 한계 요약

| 한계 | 영향 | 심각도 |
|------|------|--------|
| 단일 JVM 의존 | 서비스 분리 불가, 독립 배포 불가 | 🔴 High |
| 이벤트 유실 가능 | 데이터 불일치, 트랜잭션 미완료 | 🔴 High |
| 순서 보장 불가 | 비즈니스 로직 오류 가능 | 🟡 Medium |
| 백프레셔 부재 | 과부하 시 장애 위험 | 🟡 Medium |
| 재시도 메커니즘 없음 | 일시적 오류 복구 불가 | 🟡 Medium |

### 8.3 대응 방안 로드맵

#### 단기 (1-2개월): Outbox 패턴
```
문제: 이벤트 유실
해결: DB에 이벤트 영속화 → 재시도 가능
한계: 여전히 단일 JVM
```

#### 중기 (2-4개월): Kafka 도입 ⭐
```
문제: 서비스 간 결합, 확장성 부족
해결:
  1. 메시지 브로커 도입
  2. At-Least-Once 전달 보장
  3. 순서 보장 (Partition Key)
  4. 재시도 및 DLQ 자동화
  5. 서비스 물리적 분리

핵심: Kafka는 필수 인프라
```

#### 장기 (4-6개월): 완전한 MSA
```
목표: 도메인별 독립 DB, 독립 배포, 서비스 메시, 분산 추적
```

### 8.4 최종 대응 방안

#### 1. Spring Events는 임시 방편
- ✅ 초기 프로토타입에 적합
- ❌ 실제 MSA 환경에서는 한계 명확
- ⚠️ **메시지 큐 도입 계획을 조기에 수립**

#### 2. Kafka 도입은 선택이 아닌 필수
```
도메인 분리 계획 있다면:
→ Kafka 도입 시점 = 분리 시작 전

이유:
- 이벤트 유실 방지
- 서비스 독립성 확보
- 확장 가능한 아키텍처
```

#### 3. 단계적 전환 전략
```
Phase 1: Outbox 패턴 (즉시)
  → 이벤트 유실 방지

Phase 2: Kafka 도입 (1-2개월 내)
  → 서비스 분리 준비

Phase 3: 완전한 MSA (3-6개월)
  → 독립 DB, 독립 배포
```

#### 4. 필수 운영 역량
- Kafka 운영 노하우
- Saga 상태 관리 및 모니터링
- DLQ 처리 프로세스
- 분산 추적 및 디버깅

### 8.5 결론

**도메인별 분리는 트랜잭션 처리의 복잡도를 필연적으로 증가시킵니다.**

- 현재 Spring Events 방식은 **개념 증명(PoC) 수준**
- 실제 운영 환경에서는 **Kafka 등 메시지 큐가 필수**
- 분산 트랜잭션은 **최종 일관성, 재시도, 보상 트랜잭션**으로 대응
- 모니터링 및 추적 없이는 **장애 대응 불가능**

**결론**: 도메인별 분리를 계획할 때, 메시지 큐 도입을 우선순위로 고려해야 하며, 이를 기반으로 한 Saga 패턴, Outbox 패턴, 분산 추적 체계를 함께 구축해야 안정적인 서비스 운영이 가능합니다.
