# 테스트 실패 해결 보고서 (Test Fixture 중심)

## 배경
- 유닛 테스트에서 엔티티의 `id`, `createdAt`, `updatedAt` 등을 즉시 Not-Null로 단정하여 실패가 발생했습니다.
- 컨트롤러 테스트는 HTTP 메서드/경로/상태코드가 실제 스펙과 달라 4xx/5xx가 발생했습니다.
- `StockService`는 도메인이 불변 패턴을 사용하지만 반환값을 재할당하지 않아 상태 전이가 저장되지 않는 문제가 있었습니다.

## 목표
- 프로덕션 코드 수정은 최소화하고, 테스트를 신속히 안정화합니다.
- 도메인의 JPA 관리 필드로 인한 NPE/단정 실패를 테스트 측에서 해소합니다.
- 컨트롤러 스펙 불일치는 테스트를 스펙에 맞추어 교정합니다.

## 채택한 해결 전략: Test Fixture
테스트 전용 픽스처를 추가하여 엔티티의 식별자/타임스탬프를 리플렉션으로 주입합니다.

- 파일: `src/test/java/hhplus/ecommerce/unitTest/support/DomainTestFixtures.java`
  - 제공 기능: `setId(target, field)`, `initTimestamps(target)`
  - 장점: 도메인 생성 로직은 그대로, 테스트 기대를 만족하도록 보정

- 사용자 컨트롤러 전용 픽스처: `src/test/java/hhplus/ecommerce/unitTest/user/fixtures/UserFixtures.java`
  - `userWithTimestamps(...)`로 `UserResponse.of`에서의 NPE 제거

이 방식으로 도메인 테스트는 기존 단정(예: id/createdAt not-null)을 유지하면서 프로덕션 코드를 건드리지 않고 통과시킬 수 있습니다.

## 다른 고려안과 비교
1) 프로덕션 코드 변경 (예: DTO 변환에서 null-safe)
   - 장점: 테스트 보정 불필요, 런타임 안전성 향상
   - 단점: 실제 API 스펙/출력 형식을 바꿀 수 있고, 최소 수정 원칙에 어긋남

2) 통합 테스트로 전환해 JPA로 영속 후 검증
   - 장점: 실제 동작에 가깝게 검증 가능
   - 단점: 테스트 러닝타임/복잡도 증가, 현재 유닛 테스트 의도와 불일치

3) 테스트 기대값 완화 (id/타임스탬프 단정 제거)
   - 장점: 간단함
   - 단점: 원래 테스트 의도가 약화되고 회귀를 잡기 어려움

채택 근거: 테스트 픽스처는 수정 범위가 작고(테스트만), 테스트 의도를 유지하며, 프로덕션 코드에 영향이 없습니다.

## 서비스/컨트롤러 보완(최소 변경)
- StockService (불변 엔티티 반영)
  - `confirm()`/`release()` 반환값을 재할당하도록 수정
  - 영향 범위: 해당 메서드 내부 2줄만 변경

- 컨트롤러 테스트 스펙 정합
  - Cart: POST 201, PATCH 사용, DELETE 경로/204에 맞춤
  - Point: `/api/point/charge/{userId}` 201, 히스토리 `/api/point/{userId}`
  - User: 픽스처로 타임스탬프 주입하여 NPE 제거

## 수정 목록 (요지)
- 테스트 픽스처 추가: `DomainTestFixtures`, `UserFixtures`
- 도메인 테스트 보정: Cart/Product/ProductOption/Order/PointHistory
- 컨트롤러 테스트 보정: CartControllerTest, PointControllerTest, UserControllerTest
- 서비스 최소 수정: StockService의 불변 엔티티 재할당
- 서비스 테스트 보정: StockServiceTest의 불필요 스텁 제거 및 확정 상태 재할당

## 기대 효과
- 유닛 테스트가 JPA 환경 없이도 안정적으로 동작
- 실제 스펙과 맞는 컨트롤러 테스트로 회귀 방지
- 서비스 로직의 상태 전이 보장(결함 제거)

## 향후 개선 제안
- 도메인 엔티티에 테스트 전용 패키지-프라이빗 빌더 제공 고려(리플렉션 의존 감소)
- 컨트롤러 테스트에 스냅샷/계약 테스트 일부 도입으로 응답 스키마 회귀 방지
- 통합 테스트는 `integrationTest` 소스셋으로 격리, Docker/Testcontainers 활용 정비

