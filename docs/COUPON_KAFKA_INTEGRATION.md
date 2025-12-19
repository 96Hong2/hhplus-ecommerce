# 쿠폰 시스템 카프카 활용 설계

## 1. 배경 및 목적

### 현재 구조
- Redis ZSet + Lua Script로 선착순 제어 (RedisCouponService)
- AsyncUserCouponSaver로 비동기 DB 저장
- 단일 애플리케이션 내 이벤트 처리

### 개선 목적
- 쿠폰 발급/사용 이벤트를 Kafka로 발행하여 확장성 확보
- 알림, 통계, 외부 시스템 연동 등 추가 처리를 느슨하게 결합
- 메시지 유실 방지 및 재처리 보장
- 기존 Redis 선착순 로직은 최대한 유지

---

## 2. 핵심 설계 방향

### 2.1 기존 로직 유지
✅ **Redis ZSet + Lua Script 선착순 제어는 그대로 유지**
- 현재 구현이 이미 최적화되어 있음 (TPS 1000+, 응답 10-30ms)
- 동시성 제어와 정확성이 검증됨

### 2.2 Kafka 활용 포인트
🔹 **쿠폰 발급 이벤트**: Redis에서 발급 성공 후 Kafka로 이벤트 발행
🔹 **쿠폰 사용 이벤트**: 주문에서 쿠폰 사용 시 Kafka로 이벤트 발행
🔹 **확장 처리**: Consumer에서 알림, 통계, 외부 연동 등 처리

---

## 3. 아키텍처 변경

### 3.1 기존 구조
```
Client → Controller → RedisCouponService
                        ↓ Redis ZSet (선착순 제어)
                        ↓ DB 동기 저장
                        ↓ AsyncUserCouponSaver (비동기)
                        → 완료
```

### 3.2 변경 후 구조
```
[쿠폰 발급 플로우]
Client → Controller → RedisCouponService
                        ↓ Redis ZSet (선착순 제어, 기존 유지)
                        ↓ DB 동기 저장
                        ↓ EventPublisher.publish(CouponIssuedEvent)
                        ↓ Spring Event → CouponEventHandler
                        ↓ Kafka Producer → 'coupon-issued' 토픽
                        → Client 응답 (즉시)

[비동기 후속 처리]
Kafka 'coupon-issued' 토픽
  ↓ Consumer 1: 알림 발송 (SMS, Push)
  ↓ Consumer 2: 통계 업데이트 (발급 현황)
  ↓ Consumer 3: 외부 연동 (마케팅 플랫폼)
```

---

## 4. 패키지 구조

```
src/main/java/hhplus/ecommerce/coupon/
│
├── domain/
│   ├── model/
│   │   ├── Coupon.java                    # 기존 유지
│   │   └── UserCoupon.java                # 기존 유지
│   ├── repository/                        # 기존 유지
│   └── event/                             # 신규 추가
│       ├── CouponIssuedEvent.java         # 쿠폰 발급 이벤트
│       └── CouponUsedEvent.java           # 쿠폰 사용 이벤트
│
├── application/
│   ├── service/
│   │   ├── CouponService.java             # 기존 유지
│   │   ├── RedisCouponService.java        # 기존 유지 (Kafka 발행만 추가)
│   │   └── UserCouponService.java         # 기존 유지 (Kafka 발행만 추가)
│   └── eventhandler/                      # 신규 추가
│       └── CouponEventHandler.java        # Spring Event → Kafka 브릿지
│
├── infrastructure/                        # 신규 추가
│   └── kafka/
│       ├── config/
│       │   └── CouponKafkaConfig.java     # Kafka 토픽 설정
│       ├── producer/
│       │   └── CouponEventProducer.java   # Kafka 메시지 발행
│       └── consumer/
│           ├── CouponNotificationConsumer.java    # 알림 처리
│           └── CouponStatisticsConsumer.java      # 통계 처리
│
└── presentation/                          # 기존 유지
    └── controller/
        └── CouponController.java          # 기존 유지
```

### 패키지 구조 선택 이유

#### domain/event/
- 도메인 이벤트는 비즈니스 개념을 나타내므로 domain 레이어에 위치
- 불변 객체로 설계하여 이벤트 신뢰성 확보

#### application/eventhandler/
- Spring Event Listener 역할
- Spring Event를 받아서 Kafka Producer로 발행하는 브릿지 역할
- 도메인 레이어의 프레임워크 독립성 유지

#### infrastructure/kafka/
- 외부 메시징 시스템(Kafka) 연동은 인프라 관심사
- Producer/Consumer를 명확히 분리하여 역할 명확화
- config 패키지로 토픽 설정 중앙 관리

---

## 5. 주요 컴포넌트 구현

### 5.1 도메인 이벤트

#### CouponIssuedEvent.java
```java
@Getter
public class CouponIssuedEvent {
    private final Long userCouponId;
    private final Long userId;
    private final Long couponId;
    private final String couponName;
    private final LocalDateTime issuedAt;

    // 생성자, 정적 팩토리 메서드
}
```

**역할**: 쿠폰 발급 완료 사실을 알리는 도메인 이벤트 (불변)

#### CouponUsedEvent.java
```java
@Getter
public class CouponUsedEvent {
    private final Long userCouponId;
    private final Long userId;
    private final Long orderId;
    private final Long discountAmount;
    private final LocalDateTime usedAt;

    // 생성자, 정적 팩토리 메서드
}
```

**역할**: 쿠폰 사용 완료 사실을 알리는 도메인 이벤트 (불변)

### 5.2 이벤트 발행 (기존 서비스 수정 최소화)

#### RedisCouponService.java (수정)
```java
public UserCoupon issueCouponWithRedisZset(Long userId, Long couponId) {
    // ... 기존 Redis ZSet 로직 유지 ...

    // DB 저장
    UserCoupon userCoupon = UserCoupon.create(userId, couponId);
    try {
        UserCoupon saved = userCouponRepository.save(userCoupon);

        // 쿠폰 정보 조회 (이벤트에 포함)
        Coupon coupon = couponService.getCouponById(couponId);

        // 이벤트 발행 (신규 추가)
        eventPublisher.publish(CouponIssuedEvent.of(saved, coupon));

        return saved;
    } catch (Exception e) {
        // 기존 보상 로직 유지
        redisTemplate.opsForZSet().remove(issueKey, userId.toString());
        throw CouponException.couponIssueFailed("쿠폰 발급 중 오류가 발생했습니다.");
    }
}
```

**변경 포인트**: DB 저장 성공 후 이벤트 발행만 추가 (2줄)

#### UserCouponService.java (수정)
```java
@Transactional
public UserCoupon useCoupon(Long userCouponId, Long orderId) {
    UserCoupon userCoupon = userCouponRepository.findById(userCouponId)
            .orElseThrow(() -> CouponException.couponNotFound(userCouponId));

    userCoupon.use(orderId);
    UserCoupon saved = userCouponRepository.save(userCoupon);

    // 이벤트 발행 (신규 추가)
    eventPublisher.publish(CouponUsedEvent.of(saved));

    return saved;
}
```

**변경 포인트**: 쿠폰 사용 완료 후 이벤트 발행만 추가 (1줄)

### 5.3 이벤트 핸들러 (Spring Event → Kafka 브릿지)

#### CouponEventHandler.java
```java
@Component
@RequiredArgsConstructor
@Slf4j
public class CouponEventHandler {

    private final CouponEventProducer couponEventProducer;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCouponIssued(CouponIssuedEvent event) {
        try {
            couponEventProducer.sendCouponIssuedEvent(event);
        } catch (Exception e) {
            log.error("쿠폰 발급 이벤트 전송 실패. userCouponId={}", event.getUserCouponId(), e);
            // Kafka 전송 실패는 핵심 비즈니스에 영향 없음 (로그만 남김)
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCouponUsed(CouponUsedEvent event) {
        try {
            couponEventProducer.sendCouponUsedEvent(event);
        } catch (Exception e) {
            log.error("쿠폰 사용 이벤트 전송 실패. userCouponId={}", event.getUserCouponId(), e);
        }
    }
}
```

**역할**:
- 트랜잭션 커밋 후 Kafka로 메시지 발행
- Kafka 실패 시 예외를 삼켜서 핵심 비즈니스 영향 차단

### 5.4 Kafka Producer

#### CouponEventProducer.java
```java
@Component
@RequiredArgsConstructor
@Slf4j
public class CouponEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendCouponIssuedEvent(CouponIssuedEvent event) {
        String topic = "coupon-issued";
        String key = event.getUserId().toString(); // 동일 사용자 이벤트는 순서 보장

        kafkaTemplate.send(topic, key, event)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("쿠폰 발급 이벤트 전송 실패. userId={}, couponId={}",
                        event.getUserId(), event.getCouponId(), ex);
                } else {
                    log.info("쿠폰 발급 이벤트 전송 성공. userId={}, couponId={}",
                        event.getUserId(), event.getCouponId());
                }
            });
    }

    public void sendCouponUsedEvent(CouponUsedEvent event) {
        String topic = "coupon-used";
        String key = event.getUserId().toString();

        kafkaTemplate.send(topic, key, event)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("쿠폰 사용 이벤트 전송 실패. orderId={}", event.getOrderId(), ex);
                } else {
                    log.info("쿠폰 사용 이벤트 전송 성공. orderId={}", event.getOrderId());
                }
            });
    }
}
```

**특징**:
- 메시지 키는 userId로 설정하여 동일 사용자 이벤트 순서 보장
- 비동기 전송 결과 로깅

### 5.5 Kafka Consumer

#### CouponNotificationConsumer.java
```java
@Component
@RequiredArgsConstructor
@Slf4j
public class CouponNotificationConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
        topics = "coupon-issued",
        groupId = "coupon-notification-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeCouponIssued(CouponIssuedEvent event, Acknowledgment ack) {
        try {
            log.info("쿠폰 발급 알림 처리 시작. userId={}, couponName={}",
                event.getUserId(), event.getCouponName());

            // 알림 발송 (SMS, Push 등)
            notificationService.sendCouponIssuedNotification(
                event.getUserId(),
                event.getCouponName()
            );

            ack.acknowledge(); // 수동 커밋

        } catch (Exception e) {
            log.error("쿠폰 발급 알림 실패. userId={}", event.getUserId(), e);
            // 재시도 로직 또는 DLQ 전송
        }
    }
}
```

**특징**:
- 수동 커밋으로 처리 완료 보장
- 실패 시 재처리 가능하도록 설계

#### CouponStatisticsConsumer.java
```java
@Component
@RequiredArgsConstructor
@Slf4j
public class CouponStatisticsConsumer {

    private final CouponStatisticsService statisticsService;

    @KafkaListener(
        topics = {"coupon-issued", "coupon-used"},
        groupId = "coupon-statistics-group"
    )
    public void consumeCouponEvent(Object event, Acknowledgment ack) {
        try {
            if (event instanceof CouponIssuedEvent) {
                statisticsService.updateIssuedStats((CouponIssuedEvent) event);
            } else if (event instanceof CouponUsedEvent) {
                statisticsService.updateUsedStats((CouponUsedEvent) event);
            }

            ack.acknowledge();

        } catch (Exception e) {
            log.error("쿠폰 통계 업데이트 실패. event={}", event, e);
        }
    }
}
```

**특징**:
- 여러 토픽을 하나의 Consumer로 처리
- 발급/사용 통계를 실시간으로 업데이트

### 5.6 Kafka 설정

#### CouponKafkaConfig.java
```java
@Configuration
public class CouponKafkaConfig {

    @Bean
    public NewTopic couponIssuedTopic() {
        return TopicBuilder.name("coupon-issued")
                .partitions(3)  // 병렬 처리를 위해 3개 파티션
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic couponUsedTopic() {
        return TopicBuilder.name("coupon-used")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
```

**설정 근거**:
- 파티션 3개: Consumer 3개까지 병렬 처리 가능
- Replica 1개: 개발 환경 기준 (운영 환경에서는 2-3 권장)

---

## 6. 이벤트 흐름 예시

### 6.1 쿠폰 발급 플로우 (정상)
```
[사용자] → POST /api/coupon/{couponId}/issue-redis
          ↓
[RedisCouponService]
  ├─ Redis ZSet 선착순 제어 (기존)
  ├─ DB 저장 (동기)
  ├─ EventPublisher.publish(CouponIssuedEvent)
  └─ Client 응답 (즉시)
          ↓ (트랜잭션 커밋 후)
[CouponEventHandler]
  └─ Kafka Producer → 'coupon-issued' 토픽
          ↓
[Kafka Consumers]
  ├─ CouponNotificationConsumer → 알림 발송
  └─ CouponStatisticsConsumer → 통계 업데이트
```

### 6.2 쿠폰 사용 플로우 (주문에서 호출)
```
[주문 생성] → UserCouponService.useCoupon()
            ↓
  ├─ UserCoupon 상태 변경 (USED)
  ├─ DB 저장
  ├─ EventPublisher.publish(CouponUsedEvent)
  └─ 완료
            ↓ (트랜잭션 커밋 후)
[CouponEventHandler]
  └─ Kafka Producer → 'coupon-used' 토픽
            ↓
[CouponStatisticsConsumer]
  └─ 사용 통계 업데이트
```

---

## 7. 왜 이렇게 구현하는가?

### 7.1 기존 코드 수정 최소화
- RedisCouponService: 이벤트 발행 2줄만 추가
- UserCouponService: 이벤트 발행 1줄만 추가
- Redis 선착순 로직은 전혀 변경 없음

### 7.2 관심사 분리
- **핵심 비즈니스**: 쿠폰 발급/사용 (DB 저장까지)
- **부가 처리**: 알림, 통계 (Kafka Consumer에서 독립적 처리)
- Kafka 전송 실패해도 핵심 비즈니스 영향 없음

### 7.3 확장성
```java
// 새로운 Consumer 추가 시 기존 코드 수정 불필요
@Component
public class CouponExternalSystemConsumer {
    @KafkaListener(topics = "coupon-issued", groupId = "external-group")
    public void consume(CouponIssuedEvent event) {
        // 외부 마케팅 플랫폼 연동
    }
}
```

### 7.4 메시지 보장
- Kafka의 메시지 영속성으로 데이터 유실 방지
- Consumer 실패 시 재처리 가능 (offset 관리)
- Dead Letter Queue 패턴 적용 가능

### 7.5 성능 영향 최소화
- Kafka 전송은 비동기 (kafkaTemplate.send는 논블로킹)
- 트랜잭션 커밋 후 이벤트 핸들러 실행
- Client 응답 속도는 기존과 동일 유지

### 7.6 테스트 용이성
- 이벤트 발행 여부만 검증하면 됨
- Consumer는 독립적으로 단위 테스트 가능

---

## 8. application.properties 설정

```properties
# Kafka Producer
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer
spring.kafka.producer.acks=1
spring.kafka.producer.retries=3

# Kafka Consumer
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer
spring.kafka.consumer.auto-offset-reset=earliest
spring.kafka.consumer.enable-auto-commit=false
spring.kafka.listener.ack-mode=manual

# JSON 직렬화
spring.kafka.consumer.properties.spring.json.trusted.packages=hhplus.ecommerce.coupon.domain.event
```

---

## 9. 장점 및 트레이드오프

### 9.1 장점

✅ **기존 로직 유지**: Redis 선착순 제어는 그대로, 이벤트만 추가
✅ **확장 가능**: 새로운 Consumer 추가가 기존 코드에 영향 없음
✅ **느슨한 결합**: 알림, 통계 시스템이 독립적으로 발전 가능
✅ **메시지 보장**: Kafka의 영속성으로 데이터 유실 방지
✅ **재처리 가능**: Consumer 실패 시 offset 기반 재처리

### 9.2 트레이드오프

❗ **복잡도 증가**: 컴포넌트 수 증가 (Event, Handler, Producer, Consumer)
❗ **모니터링 필요**: Kafka Lag, Consumer 상태 추적 필요
❗ **인프라 의존성**: Kafka 서버 운영 및 관리 필요
❗ **디버깅 어려움**: 비동기 흐름 추적 어려움 (로깅 강화 필요)

---

## 10. 향후 확장 방향

### 10.1 Dead Letter Queue (DLQ)
```java
@KafkaListener(topics = "coupon-issued", groupId = "notification-group")
public void consume(CouponIssuedEvent event, Acknowledgment ack) {
    try {
        notificationService.send(event);
        ack.acknowledge();
    } catch (Exception e) {
        // DLQ로 전송
        kafkaTemplate.send("coupon-issued-dlq", event);
        ack.acknowledge();
    }
}
```

### 10.2 외부 시스템 연동
- 마케팅 플랫폼 자동 연동 (쿠폰 발급 데이터 전송)
- BI 도구 연동 등

### 10.3 이벤트 소싱 (Event Sourcing)
- 모든 쿠폰 이벤트를 영구 저장하여 감사 로그 생성
- 특정 시점 상태 재구성 가능

---

## 11. 구현 순서

1. **도메인 이벤트 정의**: CouponIssuedEvent, CouponUsedEvent
2. **Kafka 설정**: CouponKafkaConfig, Producer/Consumer 설정
3. **Producer 구현**: CouponEventProducer
4. **이벤트 핸들러**: CouponEventHandler (Spring Event → Kafka)
5. **서비스 수정**: RedisCouponService, UserCouponService에 이벤트 발행 추가
6. **Consumer 구현**: CouponNotificationConsumer, CouponStatisticsConsumer
7. **테스트**: 통합 테스트 및 Kafka 흐름 검증

---

## 12. 결론

쿠폰 시스템에 Kafka를 도입하여:
- ✅ 기존 Redis 선착순 로직은 유지하면서 확장성만 확보
- ✅ 알림, 통계 등 부가 기능을 느슨하게 결합
- ✅ 메시지 유실 방지 및 재처리 보장
- ✅ 향후 외부 시스템 연동 및 이벤트 기반 아키텍처 확장 기반 마련

기존 코드 수정은 최소화하면서도 이벤트 기반 아키텍처의 장점을 누릴 수 있는 구조입니다.

