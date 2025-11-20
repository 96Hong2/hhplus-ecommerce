# 재고 관리: 조건부 UPDATE(낙관적 스타일) 도입 보고서

## 무엇을 바꿨나
- 재고 감소를 JPQL 조건부 UPDATE로 원자 처리
  - `ProductOptionRepository.decreaseIfEnough(id, qty)`: `stockQuantity >= qty`일 때만 차감(영향 행 1)
  - `ProductOptionRepository.increaseStock(id, qty)`: 해제 시 재고 복구(영향 행 1)
- 서비스 흐름 최소 수정
  - 예약(reserve): 조건부 UPDATE로 즉시 차감 → 예약 레코드 저장(짧은 재시도 최대 3회)
  - 확정(confirm): 예약 상태만 CONFIRMED로 변경(재고 추가 차감 없음)
  - 해제(release): 예약 상태 RELEASED → 재고 복구

## 왜 @Version(낙관적 락) 대신 조건부 UPDATE?
- 단일 원자 DML로 경합 구간 단축
  - @Version은 “조회→비즈니스 검증→저장” 흐름에서 버전 충돌 시 예외/재시도 핸들링이 필요
  - 조건부 UPDATE는 DB가 한 번에 판단/차감하여 레이스 윈도우를 최소화
- 쓰기 온-focus 경합에서 높은 처리량
  - UPDATE 한 번으로 끝나므로, 동일 레코드 다중 경합 시에도 실패/성공이 빠르게 갈림
- 간결한 실패 의미
  - 영향 행 0 = “재고 부족 또는 행 미존재”로 명확히 매핑 가능(서비스에서 StockException 변환)
- 락 대기 감소(잠금 범위/시간 축소)
  - 비관적 락은 큐잉/대기 비용이 있고, @Version 재시도는 응답시간 분산이 큼

## 실패/재시도 전략
- 기본 정책
  - 첫 시도에서 영향 행 1이면 성공
  - 영향 행 0이면 즉시 `StockException.stockQuantityInsufficient(...)`
  - 예외적 경쟁 상황을 대비해 매우 짧은 재시도(최대 3회)만 허용
- 장점
  - 단순하고 예측 가능. 재시도 폭이 작아 DB 부하 통제 용이
- 트레이드오프
  - 초고경합 시 소수 요청은 재시도 후에도 실패 가능(의도된 정책)

## 테스트 관점 정합성
- 단위 테스트
  - 성공: `decreaseIfEnough(id, qty) -> 1` 스텁
  - 부족: `decreaseIfEnough(id, qty) -> 0` → `StockException` 단정
  - 해제: `increaseStock(id, qty) -> 1` 검증
  - 확정: 재고 변화 없음(상태만 CONFIRMED)
- 통합 테스트
  - “예약 시 차감, 확정 시 추가 차감 없음, 해제 시 복구” 정책 유지
  - 동시 예약 시 합계 재고 일관성 유지 확인

## 변경 범위(최소 수정)
- Repository (JPQL 추가)
  - `src/main/java/hhplus/ecommerce/product/domain/repository/ProductOptionRepository.java`
- Service (흐름만 정렬)
  - `src/main/java/hhplus/ecommerce/product/application/service/StockService.java`
- Unit Test (스텁/단정 보정)
  - `src/test/java/hhplus/ecommerce/unitTest/product/application/StockServiceTest.java`

## 운영 관점 체크리스트
- 인덱스: `productOptionId` 기반 PK/인덱스 전제(UPDATE 조건 효율)
- 모니터링: 조건부 UPDATE 실패율/재시도율, 재고 부족 예외율 대시보드화
- 타임아웃/리밋: API 레벨에서 짧은 재시도 한정, 무한 재시도 금지
- 장애 시나리오: 재고 부족을 4xx로 매핑, 재시도-가능 오류는 5xx/리트라이 정책 분리

## 요약
- 조건부 UPDATE는 “DB 단일 원자 연산”으로 재고 차감을 수행해 경합을 줄이고 처리량을 높입니다.
- @Version 대비 구현/오류 모델이 단순해 테스트와 운영 가시성이 좋습니다.
- 서비스 로직과 테스트는 정책에 맞게 최소 범위로 정렬되었습니다.

