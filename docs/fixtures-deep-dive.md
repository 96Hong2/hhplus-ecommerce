# 테스트 픽스처 깊이 파보기

테스트를 돌리다 보면 “JPA가 넣어주는 값(id, createdAt 등)을 유닛 테스트 바로 직후에 단정하고 싶다!”는 순간이 옵니다. 
하지만 엔티티는 보통 영속화(저장) 이후에야 아이디/타임스탬프가 채워지죠. 이 간극을 메워주는 게 우리가 만든 테스트 픽스처입니다.

- 공용 픽스처: `src/test/java/hhplus/ecommerce/unitTest/support/DomainTestFixtures.java`
- 사용자 전용 픽스처: `src/test/java/hhplus/ecommerce/unitTest/user/fixtures/UserFixtures.java`

## 왜 픽스처가 필요할까?
- 유닛 테스트는 “영속성 레이어 없이” 빠르게 검증하는 게 목적입니다.
- 그런데 엔티티의 `id`, `createdAt`, `updatedAt` 같은 필드는 JPA가 넣습니다.
- 그래서 테스트 동안만 “반칙 허용(리플렉션)을 살짝 써서” 값을 넣고, NPE 없이 의미 있는 단정을 하려는 겁니다.

---

## DomainTestFixtures 한 장 요약
```java
public final class DomainTestFixtures {
    public static <T> T setId(T target, String idFieldName, Object idValue)
    public static <T> T initTimestamps(T target)
    // 내부 도우미
    private static void trySetField(Object target, String fieldName, Object value)
    private static void setField(Object target, String fieldName, Object value)
    private static Field getFieldRecursive(Class<?> clazz, String name)
}
```

### 1) setId: 왜 만들었고, 왜 public static일까요?
- 목적: 테스트 중인 엔티티에 `id` 값을 심어 “id가 null이 아니다” 같은 단정을 할 수 있게 합니다.
- public: 모든 테스트 클래스에서 쉽게 쓰라고 공개했습니다.
- static: 상태를 가지지 않는 순수 유틸리티이므로 객체를 만들 필요가 없습니다. `DomainTestFixtures.setId(...)`로 바로 호출!
- 제네릭 `<T> T` 반환: “타깃 객체를 그대로 돌려줘서” 체이닝(chaining)이나 한 줄 쓰기가 편합니다.
  - 예) `Cart cart = setId(Cart.create(...), "cartId", 1L);`

### 2) initTimestamps: NPE 방지용 만능 타임스탬프 주입기
- 목적: `createdAt`, `updatedAt`, (있으면) `reservedAt`, `expiresAt`까지 한 번에 세팅해 NPE를 없애줍니다.
- 대응 방식: “있으면 넣고, 없으면 조용히 패스.”
  - 어떤 엔티티는 `reservedAt`이 없을 수 있죠. 그럴 땐 그냥 무시합니다 (실패로 만들지 않음).
- 내부적으로 `trySetField`를 써서 “부드럽게” 처리합니다.

### 3) trySetField: 왜 private static void이고, setField와 왜 분리했을까요?
- private: 외부에서 직접 호출할 필요가 없습니다. 내부 헬퍼일 뿐입니다.
- static: 상태가 없고, 어디서든 호출 가능한 순수 함수이기 때문입니다.
- void: 성공/실패 여부를 반환값으로 관리할 필요가 없습니다. 실패해도 그냥 “조용히” 넘깁니다.
- 분리 이유: `setField`는 실패 시 예외를 터뜨려 “강하게” 동작합니다. 반면 `trySetField`는 실패를 삼키고 지나가는 “부드러운” 동작이 필요할 때만 씁니다.
  - 예) `initTimestamps`는 있는 필드만 세팅해야 하므로 `trySetField`가 적합합니다.

### 4) setField: 진짜 반칙(리플렉션) 도구
- 하는 일: 리플렉션으로 `private` 필드라도 강제로 열고(`setAccessible(true)`) 값을 집어넣습니다.
- 실패 시 `RuntimeException`으로 래핑해 던집니다. 그러니 외부에선 “강하게 실패”를 원하는 경우에만 이 함수를 직접 쓰는 게 좋아요.
- `setId`처럼 꼭 세팅되어야 하는 핵심 상황에 사용합니다.

### 5) getFieldRecursive: 필드를 위아래로 샅샅이!
- 동작: 현재 클래스에서 `name`에 해당하는 필드를 찾다가 없으면 슈퍼클래스로 올라갑니다. 최상위까지 갔는데도 없으면 `NoSuchFieldException`!
- 알고리즘(쉬운 비유): “현재 집 방에서 못 찾으면 위층(부모) 방으로 올라가요. 꼭대기 다락방까지 뒤졌는데도 없으면 ‘없다!’”
- 코드 포인트:
  - `Class<?> clazz` 문법: “`어떤 타입의 Class든` 받겠다”는 제네릭 와일드카드입니다. `Class<T>`처럼 구체 타입이 아니라도 OK! 그래서 유연하게 슈퍼클래스를 오르내릴 수 있습니다.

---

## UserFixtures는 뭐가 다를까?
`src/test/java/hhplus/ecommerce/unitTest/user/fixtures/UserFixtures.java`

- 역할: `User` 전용 편의 함수입니다. `userWithTimestamps(...)`로 `createdAt/updatedAt`을 바로 채워줍니다.
- 범용 유틸(위의 DomainTestFixtures)로도 가능하지만, 사용자 테스트가 자주 쓰는 패턴이라 “간단 버전”을 따로 둔 셈입니다.

---

## 예제로 보는 사용법

### A. CartTest에서 id/타임스탬프 세팅
```java
Cart cart = Cart.create(1L, 1L, 2);
DomainTestFixtures.setId(cart, "cartId", 1L);
DomainTestFixtures.initTimestamps(cart);
// 이제 cart.getCartId(), getCreatedAt()이 null 아님을 안심하고 단정!
```

### B. UserControllerTest에서 타임스탬프 세팅
```java
User user = UserFixtures.userWithTimestamps(1L, "테스트유저", BigDecimal.ZERO, UserRole.CUSTOMER);
// 컨트롤러 응답 매핑(UserResponse.of)에서 createdAt/updatedAt NPE 방지
```

---

## 주의사항 (하지만 테스트에선 OK)
- 리플렉션은 캡슐화를 뚫습니다. 운영 코드에 쓰면 위험하지만, “테스트 전용 도구”로, “엔티티 내부 구현에 종속된 어댑터”라고 생각하면 됩니다.
- 프로덕션에서 상태를 바꾸진 않으며, 테스트 가독성과 속도를 위해 의도적으로 허용한 반칙 카드입니다.

---

## 마무리 요약
- setId: 꼭 들어가야 하는 필드는 “강하게” 세팅(예외 발생), 공용(static) 유틸.
- initTimestamps: 다양한 엔티티에 “부드럽게” 타임스탬프 세팅, 없는 필드는 조용히 패스.
- trySetField vs setField: 실패를 삼키는 유연함 vs 실패를 알려주는 엄격함.
- getFieldRecursive: 필드를 찾을 때까지 슈퍼클래스까지 훑는 탐색기.
- Class<?>: “아무 타입의 Class”도 받을 수 있는 제네릭 문법.

이제 픽스처로 유닛 테스트를 더 단단하게, 더 재밌게 작성해 봅시다! :)
