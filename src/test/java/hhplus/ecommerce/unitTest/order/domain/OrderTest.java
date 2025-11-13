package hhplus.ecommerce.unitTest.order.domain;

import hhplus.ecommerce.common.domain.exception.OrderException;
import hhplus.ecommerce.order.domain.model.Order;
import hhplus.ecommerce.order.domain.model.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class OrderTest {

    @Test
    @DisplayName("주문을 정상적으로 생성할 수 있다.")
    void createOrder() {
        // when
        Order order = Order.create("ORD20251107001", 1L, BigDecimal.valueOf(20000),
                BigDecimal.valueOf(2000), 1L);

        // then
        assertThat(order.getOrderId()).isNotNull();
        assertThat(order.getOrderNumber()).isEqualTo("ORD20251107001");
        assertThat(order.getUserId()).isEqualTo(1L);
        assertThat(order.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(20000));
        assertThat(order.getDiscountAmount()).isEqualByComparingTo(BigDecimal.valueOf(2000));
        // 최종 금액 = 20000 - 2000 = 18000
        assertThat(order.getFinalAmount()).isEqualByComparingTo(BigDecimal.valueOf(18000));
        assertThat(order.getCouponId()).isEqualTo(1L);
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getCreatedAt()).isNotNull();
        assertThat(order.getUpdatedAt()).isNotNull();
        assertThat(order.getExpiresAt()).isNotNull();
        assertThat(order.getExpiresAt()).isAfter(LocalDateTime.now());
    }

    @Test
    @DisplayName("주문 생성 시 할인 금액과 사용 포인트가 없어도 정상 생성된다.")
    void createOrderWithoutDiscountAndPoints() {
        // when
        Order order = Order.create("ORD20251107002", 1L, BigDecimal.valueOf(20000),
                null, null);

        // then
        assertThat(order.getDiscountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(order.getFinalAmount()).isEqualByComparingTo(BigDecimal.valueOf(20000));
    }

    @Test
    @DisplayName("주문 생성 시 주문번호가 없으면 예외가 발생한다.")
    void createOrderWithNullOrderNumber() {
        // when & then
        assertThatThrownBy(() -> Order.create(null, 1L, BigDecimal.valueOf(20000),
                null, null))
                .isInstanceOf(OrderException.class)
                .hasMessageContaining("주문번호는 필수값입니다.");

        assertThatThrownBy(() -> Order.create("", 1L, BigDecimal.valueOf(20000),
                null, null))
                .isInstanceOf(OrderException.class)
                .hasMessageContaining("주문번호는 필수값입니다.");
    }

    @Test
    @DisplayName("주문 생성 시 유저ID가 없으면 예외가 발생한다.")
    void createOrderWithNullUserId() {
        // when & then
        assertThatThrownBy(() -> Order.create("ORD20251107001", null, BigDecimal.valueOf(20000),
                null, null))
                .isInstanceOf(OrderException.class)
                .hasMessageContaining("유저ID는 필수값입니다.");
    }

    @Test
    @DisplayName("주문 생성 시 총 금액이 0원 이하면 예외가 발생한다.")
    void createOrderWithInvalidTotalAmount() {
        // when & then
        assertThatThrownBy(() -> Order.create("ORD20251107001", 1L, BigDecimal.ZERO,
                null, null))
                .isInstanceOf(OrderException.class)
                .hasMessageContaining("총 합계값이 유효하지 않습니다.");

        assertThatThrownBy(() -> Order.create("ORD20251107001", 1L, BigDecimal.valueOf(-1000),
                null, null))
                .isInstanceOf(OrderException.class)
                .hasMessageContaining("총 합계값이 유효하지 않습니다.");
    }

    @Test
    @DisplayName("주문 생성 시 할인 금액이 음수면 예외가 발생한다.")
    void createOrderWithNegativeDiscountAmount() {
        // when & then
        assertThatThrownBy(() -> Order.create("ORD20251107001", 1L, BigDecimal.valueOf(20000),
                BigDecimal.valueOf(-1000), null))
                .isInstanceOf(OrderException.class)
                .hasMessageContaining("할인 금액은 0원 이상이어야 합니다.");
    }

    @Test
    @DisplayName("주문 생성 시 최종 결제 금액이 0원 미만이면 예외가 발생한다.")
    void createOrderWithNegativeFinalAmount() {
        // when & then - 총금액 10000원, 할인 11000원 -> 최종 금액 -1000원
        assertThatThrownBy(() -> Order.create("ORD20251107001", 1L, BigDecimal.valueOf(10000),
                BigDecimal.valueOf(11000), null))
                .isInstanceOf(OrderException.class)
                .hasMessageContaining("최종 결제 금액이 0원 미만입니다.");
    }

    @Test
    @DisplayName("주문 상태를 변경할 수 있다.")
    void updateOrderStatus() {
        // given
        Order order = Order.create("ORD20251107001", 1L, BigDecimal.valueOf(20000),
                null, null);

        // when
        Order paidOrder = order.updateStatus(OrderStatus.PAID);

        // then
        assertThat(paidOrder.getOrderStatus()).isEqualTo(OrderStatus.PAID);
        // updatedAt는 생성 시간과 같거나 이후여야 함
        assertThat(paidOrder.getUpdatedAt()).isAfterOrEqualTo(order.getUpdatedAt());
        // 불변 객체이므로 원본은 변경되지 않음
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    @DisplayName("주문 상태 변경 시 null이면 예외가 발생한다.")
    void updateOrderStatusWithNull() {
        // given
        Order order = Order.create("ORD20251107001", 1L, BigDecimal.valueOf(20000),
                null, null);

        // when & then
        assertThatThrownBy(() -> order.updateStatus(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("주문 상태는 필수입니다.");
    }

    @Test
    @DisplayName("주문 상태 변경 시 동일한 상태로 변경하면 예외가 발생한다.")
    void updateOrderStatusWithSameStatus() {
        // given
        Order order = Order.create("ORD20251107001", 1L, BigDecimal.valueOf(20000),
                null, null);

        // when & then
        assertThatThrownBy(() -> order.updateStatus(OrderStatus.PENDING))
                .isInstanceOf(OrderException.class);
    }

    @Test
    @DisplayName("주문을 결제 완료 처리할 수 있다.")
    void payOrder() {
        // given
        Order order = Order.create("ORD20251107001", 1L, BigDecimal.valueOf(20000),
                null, null);

        // when
        Order paidOrder = order.pay();

        // then
        assertThat(paidOrder.getOrderStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(paidOrder.isPaid()).isTrue();
    }

    @Test
    @DisplayName("이미 결제 완료된 주문을 결제하면 예외가 발생한다.")
    void payAlreadyPaidOrder() {
        // given
        Order order = Order.create("ORD20251107001", 1L, BigDecimal.valueOf(20000),
                null, null);
        Order paidOrder = order.pay();

        // when & then
        assertThatThrownBy(paidOrder::pay)
                .isInstanceOf(OrderException.class)
                .hasMessageContaining("이미 결제 완료된 주문입니다.");
    }

    @Test
    @DisplayName("취소된 주문을 결제하면 예외가 발생한다.")
    void payCancelledOrder() {
        // given
        Order order = Order.create("ORD20251107001", 1L, BigDecimal.valueOf(20000),
                null, null);
        Order cancelledOrder = order.cancel();

        // when & then
        assertThatThrownBy(cancelledOrder::pay)
                .isInstanceOf(OrderException.class)
                .hasMessageContaining("이미 취소된 주문입니다.");
    }

    @Test
    @DisplayName("만료된 주문을 결제하면 예외가 발생한다.")
    void payExpiredOrder() throws InterruptedException {
        // given - 생성자를 이용하여 만료 시간을 1초 후로 설정
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusSeconds(1);
        Order order = new Order(1L, "ORD20251107001", 1L, BigDecimal.valueOf(20000),
                BigDecimal.ZERO, BigDecimal.valueOf(20000), null, OrderStatus.PENDING, expiresAt);

        // 2초 대기하여 만료시킴
        Thread.sleep(2000);

        // when & then
        assertThatThrownBy(order::pay)
                .isInstanceOf(OrderException.class)
                .hasMessageContaining("주문 시간이 초과되었습니다.");
    }

    @Test
    @DisplayName("주문을 취소할 수 있다.")
    void cancelOrder() {
        // given
        Order order = Order.create("ORD20251107001", 1L, BigDecimal.valueOf(20000),
                null, null);

        // when
        Order cancelledOrder = order.cancel();

        // then
        assertThat(cancelledOrder.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(cancelledOrder.isCancelled()).isTrue();
    }

    @Test
    @DisplayName("이미 취소된 주문을 취소하면 예외가 발생한다.")
    void cancelAlreadyCancelledOrder() {
        // given
        Order order = Order.create("ORD20251107001", 1L, BigDecimal.valueOf(20000),
                null, null);
        Order cancelledOrder = order.cancel();

        // when & then
        assertThatThrownBy(cancelledOrder::cancel)
                .isInstanceOf(OrderException.class)
                .hasMessageContaining("이미 취소된 주문입니다.");
    }

    @Test
    @DisplayName("결제 완료된 주문은 취소할 수 없다.")
    void cancelPaidOrder() {
        // given
        Order order = Order.create("ORD20251107001", 1L, BigDecimal.valueOf(20000),
                null, null);
        Order paidOrder = order.pay();

        // when & then
        assertThatThrownBy(paidOrder::cancel)
                .isInstanceOf(OrderException.class)
                .hasMessageContaining("주문 취소가 불가능한 상태입니다.");
    }

    @Test
    @DisplayName("주문 만료 여부를 확인할 수 있다.")
    void isExpired() throws InterruptedException {
        // given - 1초 후 만료되는 주문
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusSeconds(1);
        Order order = new Order(1L, "ORD20251107001", 1L, BigDecimal.valueOf(20000),
                BigDecimal.ZERO, BigDecimal.valueOf(20000), null, OrderStatus.PENDING, expiresAt);

        // when & then - 처음에는 만료되지 않음
        assertThat(order.isExpired()).isFalse();

        // 2초 대기하여 만료시킴
        Thread.sleep(2000);

        // 이제 만료됨
        assertThat(order.isExpired()).isTrue();
    }

    @Test
    @DisplayName("결제 가능 여부를 확인할 수 있다.")
    void canPayment() throws InterruptedException {
        // given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusSeconds(1);
        Order order = new Order(1L, "ORD20251107001", 1L, BigDecimal.valueOf(20000),
                BigDecimal.ZERO, BigDecimal.valueOf(20000), null, OrderStatus.PENDING, expiresAt);

        // when & then - PENDING 상태이고 만료되지 않았으면 결제 가능
        assertThat(order.canPayment()).isTrue();

        // 결제 완료 후에는 결제 불가
        Order paidOrder = order.pay();
        assertThat(paidOrder.canPayment()).isFalse();

        // 2초 대기하여 만료시킴
        Thread.sleep(2000);

        // 만료된 경우 결제 불가
        assertThat(order.canPayment()).isFalse();
    }

    @Test
    @DisplayName("주문 상태를 확인할 수 있다.")
    void checkOrderStatus() {
        // given
        Order pendingOrder = Order.create("ORD20251107001", 1L, BigDecimal.valueOf(20000),
                null, null);

        // when & then
        assertThat(pendingOrder.isPending()).isTrue();
        assertThat(pendingOrder.isPaid()).isFalse();
        assertThat(pendingOrder.isCancelled()).isFalse();

        // 결제 완료
        Order paidOrder = pendingOrder.pay();
        assertThat(paidOrder.isPending()).isFalse();
        assertThat(paidOrder.isPaid()).isTrue();
        assertThat(paidOrder.isCancelled()).isFalse();

        // 취소
        Order cancelledOrder = pendingOrder.cancel();
        assertThat(cancelledOrder.isPending()).isFalse();
        assertThat(cancelledOrder.isPaid()).isFalse();
        assertThat(cancelledOrder.isCancelled()).isTrue();
    }
}
