# 이벤트 기반 Saga 패턴 전환 보고서

## 1. 변경 개요

**목적**: 오케스트레이션 Saga 패턴에서 이벤트 기반 코레오그래피 패턴으로 전환하여 관심사 분리 및 확장성 향상

**변경 범위**:
- 주문 생성 UseCase
- 외부 시스템 연동 처리
- 보상 트랜잭션 처리

---

## 2. 기존 구조의 문제점

### 2.1 강한 결합
```java
public class CreateOrderUseCase {
    public OrderCreateResponse execute() {
        // 1. 주문 생성 (핵심 비즈니스)
        createOrderTransaction();

        // 2. 외부 ERP 연동 (부가 로직)
        externalIntegrationService.sendOrderToERP();

        // 3. 실패 시 보상 (부가 로직)
        try/catch로 명시적 보상 처리
    }
}
```

**문제점**:
- UseCase가 핵심 비즈니스 + 외부 연동 + 보상 처리를 모두 담당
- 새로운 후속 처리 추가 시 UseCase 수정 필요
- 테스트 복잡도 증가

### 2.2 확장성 부족
- 알림 발송, 통계 업데이트 등 새로운 처리 추가 시 UseCase 변경 불가피
- 외부 시스템이 늘어날수록 UseCase 복잡도 증가

---

## 3. 변경된 아키텍처

### 3.1 이벤트 기반 코레오그래피 패턴

```
[주문 생성 트랜잭션]
CreateOrderUseCase.execute()
  → 주문 생성
  → OrderCreatedEvent 발행
  → 트랜잭션 COMMIT

[이벤트 핸들러 1] @TransactionalEventListener(AFTER_COMMIT)
OrderIntegrationEventHandler.handleOrderCreated()
  → 외부 ERP 연동
  → 실패 시: OrderIntegrationFailedEvent 발행

[이벤트 핸들러 2]
OrderCompensationEventHandler.handleIntegrationFailed()
  → 주문 취소 + 재고 복구
  → OrderCompensatedEvent 발행

[이벤트 핸들러 3]
OrderNotificationEventHandler.handleOrderCompensated()
  → 사용자 알림 발송
```

### 3.2 주요 변경 사항

| 구분 | 기존 | 변경 후 |
|------|------|---------|
| **주문 생성** | UseCase가 모든 로직 처리 | 주문 생성 + 이벤트 발행만 |
| **외부 연동** | UseCase에서 직접 호출 | 이벤트 핸들러가 자율 처리 |
| **보상 처리** | UseCase의 try-catch | 이벤트 핸들러가 자율 처리 |
| **트랜잭션** | 중앙 집중식 | 각 핸들러가 독립 관리 |

---

## 4. 패키지 구조

```
src/main/java/hhplus/ecommerce/
│
├── order/
│   ├── domain/
│   │   └── event/                          # 도메인 이벤트
│   │       ├── OrderCreatedEvent.java
│   │       └── OrderCompensatedEvent.java
│   │
│   ├── application/
│   │   ├── usecase/
│   │   │   └── CreateOrderUseCase.java     # 단순화됨
│   │   ├── service/
│   │   │   ├── OrderService.java
│   │   │   └── OrderCompensationService.java  # 보상 로직 분리
│   │   └── eventhandler/
│   │       ├── OrderCompensationEventHandler.java
│   │       └── OrderNotificationEventHandler.java
│   └── ...
│
├── integration/
│   ├── domain/
│   │   └── event/
│   │       └── OrderIntegrationFailedEvent.java
│   └── application/
│       ├── service/
│       │   └── ExternalIntegrationService.java
│       └── eventhandler/
│           └── OrderIntegrationEventHandler.java
│
└── common/
    └── event/
        └── EventPublisher.java             # 이벤트 발행 추상화
```

**설계 근거**:
- **도메인 이벤트**: `domain/event`에 배치 (비즈니스 의미를 담은 도메인 개념)
- **이벤트 핸들러**: `application/eventhandler`에 배치 (여러 서비스를 조율하는 애플리케이션 로직)
- **통합 분리**: `integration` 패키지로 외부 시스템 관심사 격리

---

## 5. 핵심 컴포넌트

### 5.1 도메인 이벤트

#### OrderCreatedEvent
```java
@Getter
public class OrderCreatedEvent {
    private final Long orderId;
    private final String orderNumber;
    private final Long userId;
    private final List<OrderItemInfo> orderItems;
    private final LocalDateTime occurredAt;
}
```

**역할**: 주문 생성 완료를 알리는 도메인 이벤트 (불변 객체)

#### OrderIntegrationFailedEvent
```java
@Getter
public class OrderIntegrationFailedEvent {
    private final Long orderId;
    private final String failureReason;
    private final String exceptionMessage;
    private final LocalDateTime occurredAt;
}
```

**역할**: 외부 연동 실패를 알리고 보상 트랜잭션 트리거

### 5.2 이벤트 핸들러

#### OrderIntegrationEventHandler
```java
@Component
public class OrderIntegrationEventHandler {

    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void handleOrderCreated(OrderCreatedEvent event) {
        // 주문 생성 트랜잭션 커밋 후 실행
        try {
            externalIntegrationService.sendOrderToERP(order);
        } catch (IntegrationException e) {
            eventPublisher.publish(new OrderIntegrationFailedEvent(...));
        }
    }
}
```

**특징**:
- `@TransactionalEventListener(AFTER_COMMIT)`: 트랜잭션 커밋 후 실행 보장
- 실패 시 보상 이벤트 발행 (예외는 throw하지 않음)

#### OrderCompensationEventHandler
```java
@Component
public class OrderCompensationEventHandler {

    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void handleIntegrationFailed(OrderIntegrationFailedEvent event) {
        orderCompensationService.compensateOrder(event.getOrderId());
        eventPublisher.publish(new OrderCompensatedEvent(...));
    }
}
```

**특징**:
- 보상 로직을 `OrderCompensationService`로 위임
- 보상 완료 후 추가 이벤트 발행 (알림 등)

### 5.3 EventPublisher 래핑

```java
@Component
public class EventPublisher {
    private final ApplicationEventPublisher applicationEventPublisher;

    public void publish(Object event) {
        applicationEventPublisher.publishEvent(event);
    }
}
```

**래핑 이유**:
1. **도메인 레이어의 프레임워크 독립성**: Spring 의존성 제거
2. **테스트 용이성**: Mock 생성 간편
3. **확장 가능성**: 로깅, 메트릭, 이벤트 저장 등 부가 기능 추가 용이
4. **구현체 교체 가능**: Spring Events → Kafka/RabbitMQ 전환 시 호출 코드 변경 불필요

---

## 6. 트랜잭션 전략

### 6.1 메인 트랜잭션
```java
@Transactional(isolation = REPEATABLE_READ)
public OrderCreateResponse execute() {
    // 주문 생성
    // 이벤트 발행
    // 트랜잭션 COMMIT
    // → AFTER_COMMIT 핸들러 실행
}
```

### 6.2 이벤트 핸들러 트랜잭션
```java
@TransactionalEventListener(phase = AFTER_COMMIT)
public void handleOrderCreated() {
    // 별도 트랜잭션 (메인 트랜잭션과 독립)
    // 실패해도 주문 생성 트랜잭션에 영향 없음
}
```

**핵심**:
- 메인 트랜잭션 커밋 후 이벤트 핸들러 실행
- 각 핸들러는 독립적인 트랜잭션 (실패 격리)

---

## 7. 이벤트 흐름 예시

### 7.1 정상 플로우
```
[사용자] → [CreateOrderUseCase]
            ↓ 주문 생성 + 이벤트 발행
            ↓ 트랜잭션 커밋
            ↓
[OrderIntegrationEventHandler]
            ↓ 외부 ERP 연동 성공
            ✓ 완료
```

### 7.2 외부 연동 실패 플로우
```
[사용자] → [CreateOrderUseCase]
            ↓ 주문 생성 + OrderCreatedEvent
            ↓ 트랜잭션 커밋
            ↓
[OrderIntegrationEventHandler]
            ↓ 외부 ERP 연동 실패
            ↓ OrderIntegrationFailedEvent 발행
            ↓
[OrderCompensationEventHandler]
            ↓ 주문 취소 + 재고 복구
            ↓ OrderCompensatedEvent 발행
            ↓
[OrderNotificationEventHandler]
            ↓ 사용자 알림 발송
            ✓ 완료
```

---

## 8. 장점 및 트레이드오프

### 8.1 장점

#### 관심사 분리
- CreateOrderUseCase: 주문 생성만 책임
- OrderIntegrationEventHandler: 외부 연동만 책임
- 각 컴포넌트가 단일 책임 원칙(SRP) 준수

#### 확장성
```java
// 새로운 이벤트 리스너 추가 시 기존 코드 수정 불필요
@Component
public class OrderStatisticsEventHandler {
    @TransactionalEventListener
    public void handleOrderCreated(OrderCreatedEvent event) {
        // 통계 업데이트 (기존 코드 변경 없음)
    }
}
```

#### 테스트 용이성
- 각 컴포넌트를 독립적으로 단위 테스트 가능
- 이벤트 발행 여부만 검증하면 됨

### 8.2 트레이드오프

#### 복잡도 증가
- 이벤트, 핸들러, 서비스 등 컴포넌트 수 증가
- 이벤트 흐름 추적 어려움 (분산 추적 필요)

#### 디버깅 어려움
- 명시적 호출이 아닌 이벤트 기반 → 호출 스택 추적 어려움
- 로깅 및 모니터링 강화 필요

#### 트랜잭션 복잡도
- `@TransactionalEventListener`의 동작 이해 필요
- 테스트 환경에서 트랜잭션 동기화 이슈 가능

---

## 9. 구현 시 주의사항

### 9.1 이벤트 발행 위치
```java
// ✅ 올바른 방법: 트랜잭션 안에서 발행
@Transactional
public OrderCreateResponse execute() {
    // 주문 생성
    eventPublisher.publish(event);  // 트랜잭션 안
    return response;
}

// ❌ 잘못된 방법: 트랜잭션 밖에서 발행
public OrderCreateResponse execute() {
    OrderCreateResponse response = createOrderTransaction();
    eventPublisher.publish(event);  // 트랜잭션 밖 → AFTER_COMMIT 동작 안 함
    return response;
}
```

### 9.2 이벤트 핸들러 예외 처리
```java
@TransactionalEventListener
public void handleOrderCreated(OrderCreatedEvent event) {
    try {
        externalIntegrationService.sendOrderToERP(order);
    } catch (IntegrationException e) {
        // 보상 이벤트 발행 (예외 삼킴)
        eventPublisher.publish(new OrderIntegrationFailedEvent(...));
        // throw하지 않음 → 다른 리스너 실행 보장
    }
}
```

### 9.3 이벤트 불변성
```java
// ✅ 올바른 방법: final 필드, 불변 객체
@Getter
public class OrderCreatedEvent {
    private final Long orderId;  // final
    private final List<OrderItemInfo> orderItems;  // 방어적 복사 고려
}
```

---

## 10. 향후 확장 방향

### 10.1 이벤트 저장소 (Event Store)
```java
@Component
public class EventStoreHandler {
    @EventListener
    public void storeEvent(Object event) {
        eventStoreRepository.save(event);  // 감사 로그
    }
}
```

### 10.2 비동기 이벤트 처리
```java
// 현재: 동기 처리 (같은 JVM)
@TransactionalEventListener
public void handleOrderCreated() { }

// 향후: 비동기 메시징 (Kafka, RabbitMQ)
@KafkaListener(topics = "order-created")
public void handleOrderCreated() { }
```

### 10.3 Dead Letter Queue (DLQ)
```java
@TransactionalEventListener
public void handleIntegrationFailed(OrderIntegrationFailedEvent event) {
    try {
        compensate();
    } catch (Exception e) {
        // DLQ로 전송 → 수동 처리
        deadLetterQueueService.send(event);
    }
}
```

---

## 11. 결론

오케스트레이션 Saga에서 이벤트 기반 코레오그래피로 전환하여:
- ✅ 관심사 완전 분리 (주문 생성 ↔ 외부 연동 ↔ 보상 처리)
- ✅ 확장 가능한 구조 (새로운 이벤트 리스너 추가 용이)
- ✅ 각 컴포넌트의 독립성 및 재사용성 향상

이벤트 기반 아키텍처는 복잡도가 증가하지만, 장기적으로 유지보수성과 확장성에서 큰 이점을 제공합니다.
