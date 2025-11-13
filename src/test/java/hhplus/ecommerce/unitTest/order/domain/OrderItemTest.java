package hhplus.ecommerce.unitTest.order.domain;

import hhplus.ecommerce.common.domain.exception.OrderException;
import hhplus.ecommerce.order.domain.model.OrderItem;
import hhplus.ecommerce.order.domain.model.OrderItemStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@DisplayName("OrderItem 도메인 단위 테스트")
class OrderItemTest {

    @Test
    @DisplayName("주문 항목 정상 생성 - 간편 생성자")
    void createOrderItemSuccess() {
        // given
        Long orderId = 1L;
        Long productId = 100L;
        Long productOptionId = 200L;
        String productName = "테스트 상품";
        String optionName = "사이즈-M";
        BigDecimal unitPrice = new BigDecimal("10000");
        int quantity = 2;

        // when
        OrderItem orderItem = new OrderItem(
            orderId, productId, productOptionId,
            productName, optionName, unitPrice, quantity
        );

        // then
        assertThat(orderItem).isNotNull();
        assertThat(orderItem.getOrderId()).isEqualTo(orderId);
        assertThat(orderItem.getProductId()).isEqualTo(productId);
        assertThat(orderItem.getProductOptionId()).isEqualTo(productOptionId);
        assertThat(orderItem.getProductName()).isEqualTo(productName);
        assertThat(orderItem.getOptionName()).isEqualTo(optionName);
        assertThat(orderItem.getUnitPrice()).isEqualTo(unitPrice);
        assertThat(orderItem.getQuantity()).isEqualTo(quantity);
        assertThat(orderItem.getSubtotal()).isEqualTo(new BigDecimal("20000"));
        assertThat(orderItem.getItemStatus()).isEqualTo(OrderItemStatus.PREPARING);
    }

    @Test
    @DisplayName("주문 항목 생성 실패 - null orderId")
    void createOrderItemWithNullOrderId() {
        // given
        Long orderId = null;
        Long productId = 100L;
        Long productOptionId = 200L;
        String productName = "테스트 상품";
        String optionName = "사이즈-M";
        BigDecimal unitPrice = new BigDecimal("10000");
        int quantity = 2;

        // when & then
        assertThatThrownBy(() -> new OrderItem(
            orderId, productId, productOptionId,
            productName, optionName, unitPrice, quantity
        ))
            .isInstanceOf(OrderException.class)
            .hasMessageContaining("주문 ID는 필수입니다");
    }

    @Test
    @DisplayName("주문 항목 생성 실패 - null productId")
    void createOrderItemWithNullProductId() {
        // when & then
        assertThatThrownBy(() -> new OrderItem(
            1L, null, 200L,
            "테스트 상품", "사이즈-M", new BigDecimal("10000"), 2
        ))
            .isInstanceOf(OrderException.class)
            .hasMessageContaining("상품 ID는 필수입니다");
    }

    @Test
    @DisplayName("주문 항목 생성 실패 - null productOptionId")
    void createOrderItemWithNullProductOptionId() {
        // when & then
        assertThatThrownBy(() -> new OrderItem(
            1L, 100L, null,
            "테스트 상품", "사이즈-M", new BigDecimal("10000"), 2
        ))
            .isInstanceOf(OrderException.class)
            .hasMessageContaining("상품 옵션 ID는 필수입니다");
    }

    @Test
    @DisplayName("주문 항목 생성 실패 - null productName")
    void createOrderItemWithNullProductName() {
        // when & then
        assertThatThrownBy(() -> new OrderItem(
            1L, 100L, 200L,
            null, "사이즈-M", new BigDecimal("10000"), 2
        ))
            .isInstanceOf(OrderException.class)
            .hasMessageContaining("상품명은 필수입니다");
    }

    @Test
    @DisplayName("주문 항목 생성 실패 - 빈 productName")
    void createOrderItemWithEmptyProductName() {
        // when & then
        assertThatThrownBy(() -> new OrderItem(
            1L, 100L, 200L,
            "  ", "사이즈-M", new BigDecimal("10000"), 2
        ))
            .isInstanceOf(OrderException.class)
            .hasMessageContaining("상품명은 필수입니다");
    }

    @Test
    @DisplayName("주문 항목 생성 실패 - null optionName")
    void createOrderItemWithNullOptionName() {
        // when & then
        assertThatThrownBy(() -> new OrderItem(
            1L, 100L, 200L,
            "테스트 상품", null, new BigDecimal("10000"), 2
        ))
            .isInstanceOf(OrderException.class)
            .hasMessageContaining("옵션명은 필수입니다");
    }

    @Test
    @DisplayName("주문 항목 생성 실패 - 음수 가격")
    void createOrderItemWithNegativePrice() {
        // when & then
        assertThatThrownBy(() -> new OrderItem(
            1L, 100L, 200L,
            "테스트 상품", "사이즈-M", new BigDecimal("-1000"), 2
        ))
            .isInstanceOf(OrderException.class)
            .hasMessageContaining("상품 가격은 0 이상이어야 합니다");
    }

    @Test
    @DisplayName("주문 항목 생성 실패 - 수량 0")
    void createOrderItemWithZeroQuantity() {
        // when & then
        assertThatThrownBy(() -> new OrderItem(
            1L, 100L, 200L,
            "테스트 상품", "사이즈-M", new BigDecimal("10000"), 0
        ))
            .isInstanceOf(OrderException.class)
            .hasMessageContaining("수량은 1 이상이어야 합니다");
    }

    @Test
    @DisplayName("주문 항목 생성 실패 - 음수 수량")
    void createOrderItemWithNegativeQuantity() {
        // when & then
        assertThatThrownBy(() -> new OrderItem(
            1L, 100L, 200L,
            "테스트 상품", "사이즈-M", new BigDecimal("10000"), -1
        ))
            .isInstanceOf(OrderException.class)
            .hasMessageContaining("수량은 1 이상이어야 합니다");
    }

    @Test
    @DisplayName("subtotal 계산 검증")
    void validateSubtotalCalculation() {
        // given
        BigDecimal unitPrice = new BigDecimal("15000");
        int quantity = 3;

        // when
        OrderItem orderItem = new OrderItem(
            1L, 100L, 200L,
            "테스트 상품", "사이즈-L", unitPrice, quantity
        );

        // then
        assertThat(orderItem.getSubtotal()).isEqualTo(new BigDecimal("45000"));
    }

    @Test
    @DisplayName("상태 변경 성공 - PREPARING -> SHIPPING")
    void changeStatusFromPreparingToShipping() {
        // given
        OrderItem orderItem = new OrderItem(
            1L, 100L, 200L,
            "테스트 상품", "사이즈-M", new BigDecimal("10000"), 2
        );

        // when
        OrderItem updatedItem = orderItem.changeStatus(OrderItemStatus.SHIPPING);

        // then
        assertThat(updatedItem.getItemStatus()).isEqualTo(OrderItemStatus.SHIPPING);
        assertThat(orderItem.getItemStatus()).isEqualTo(OrderItemStatus.PREPARING); // 원본 불변 확인
    }

    @Test
    @DisplayName("상태 변경 성공 - PREPARING -> CANCELLED")
    void changeStatusFromPreparingToCancelled() {
        // given
        OrderItem orderItem = new OrderItem(
            1L, 100L, 200L,
            "테스트 상품", "사이즈-M", new BigDecimal("10000"), 2
        );

        // when
        OrderItem cancelledItem = orderItem.changeStatus(OrderItemStatus.CANCELLED);

        // then
        assertThat(cancelledItem.getItemStatus()).isEqualTo(OrderItemStatus.CANCELLED);
    }

    @Test
    @DisplayName("상태 변경 실패 - SHIPPING -> CANCELLED (불가능)")
    void changeStatusFromShippingToCancelledShouldFail() {
        // given
        OrderItem orderItem = new OrderItem(
            1L, 1L, 100L, 200L,
            "테스트 상품", "사이즈-M", new BigDecimal("10000"),
            2, new BigDecimal("20000"), OrderItemStatus.SHIPPING
        );

        // when & then
        assertThatThrownBy(() -> orderItem.changeStatus(OrderItemStatus.CANCELLED))
            .isInstanceOf(OrderException.class)
            .hasMessageContaining("유효하지 않은 주문 상품 상태 변경입니다");
    }

    @Test
    @DisplayName("상태 변경 실패 - DELIVERED -> CANCELLED (불가능)")
    void changeStatusFromDeliveredToCancelledShouldFail() {
        // given
        OrderItem orderItem = new OrderItem(
            1L, 1L, 100L, 200L,
            "테스트 상품", "사이즈-M", new BigDecimal("10000"),
            2, new BigDecimal("20000"), OrderItemStatus.DELIVERED
        );

        // when & then
        assertThatThrownBy(() -> orderItem.changeStatus(OrderItemStatus.CANCELLED))
            .isInstanceOf(OrderException.class)
            .hasMessageContaining("유효하지 않은 주문 상품 상태 변경입니다");
    }

    @Test
    @DisplayName("상태 변경 실패 - null 상태로 변경")
    void changeStatusToNullShouldFail() {
        // given
        OrderItem orderItem = new OrderItem(
            1L, 100L, 200L,
            "테스트 상품", "사이즈-M", new BigDecimal("10000"), 2
        );

        // when & then
        assertThatThrownBy(() -> orderItem.changeStatus(null))
            .isInstanceOf(OrderException.class)
            .hasMessageContaining("유효하지 않은 주문 상품 상태 변경입니다");
    }

    @Test
    @DisplayName("동일한 상태로 변경 시 동일 인스턴스 반환")
    void changeStatusToSameStatusReturnsSameInstance() {
        // given
        OrderItem orderItem = new OrderItem(
            1L, 100L, 200L,
            "테스트 상품", "사이즈-M", new BigDecimal("10000"), 2
        );

        // when
        OrderItem sameItem = orderItem.changeStatus(OrderItemStatus.PREPARING);

        // then
        assertThat(sameItem).isSameAs(orderItem);
    }

    @Test
    @DisplayName("취소 성공 - PREPARING 상태")
    void cancelSuccess() {
        // given
        OrderItem orderItem = new OrderItem(
            1L, 100L, 200L,
            "테스트 상품", "사이즈-M", new BigDecimal("10000"), 2
        );

        // when
        OrderItem cancelledItem = orderItem.cancel();

        // then
        assertThat(cancelledItem.getItemStatus()).isEqualTo(OrderItemStatus.CANCELLED);
    }

    @Test
    @DisplayName("취소 실패 - SHIPPING 상태")
    void cancelFailWhenShipping() {
        // given
        OrderItem orderItem = new OrderItem(
            1L, 1L, 100L, 200L,
            "테스트 상품", "사이즈-M", new BigDecimal("10000"),
            2, new BigDecimal("20000"), OrderItemStatus.SHIPPING
        );

        // when & then
        assertThatThrownBy(orderItem::cancel)
            .isInstanceOf(OrderException.class)
            .hasMessageContaining("유효하지 않은 주문 상품 상태 변경입니다");
    }

    @Test
    @DisplayName("배송 시작 성공 - PREPARING -> SHIPPING")
    void startShippingSuccess() {
        // given
        OrderItem orderItem = new OrderItem(
            1L, 100L, 200L,
            "테스트 상품", "사이즈-M", new BigDecimal("10000"), 2
        );

        // when
        OrderItem shippingItem = orderItem.startShipping();

        // then
        assertThat(shippingItem.getItemStatus()).isEqualTo(OrderItemStatus.SHIPPING);
    }

    @Test
    @DisplayName("배송 시작 실패 - SHIPPING 상태에서 시도")
    void startShippingFailWhenAlreadyShipping() {
        // given
        OrderItem orderItem = new OrderItem(
            1L, 1L, 100L, 200L,
            "테스트 상품", "사이즈-M", new BigDecimal("10000"),
            2, new BigDecimal("20000"), OrderItemStatus.SHIPPING
        );

        // when & then
        assertThatThrownBy(orderItem::startShipping)
            .isInstanceOf(OrderException.class)
            .hasMessageContaining("유효하지 않은 주문 상품 상태 변경입니다");
    }

    @Test
    @DisplayName("배송 완료 성공 - SHIPPING -> DELIVERED")
    void completeDeliverySuccess() {
        // given
        OrderItem orderItem = new OrderItem(
            1L, 1L, 100L, 200L,
            "테스트 상품", "사이즈-M", new BigDecimal("10000"),
            2, new BigDecimal("20000"), OrderItemStatus.SHIPPING
        );

        // when
        OrderItem deliveredItem = orderItem.completeDelivery();

        // then
        assertThat(deliveredItem.getItemStatus()).isEqualTo(OrderItemStatus.DELIVERED);
    }

    @Test
    @DisplayName("배송 완료 실패 - PREPARING 상태에서 시도")
    void completeDeliveryFailWhenPreparing() {
        // given
        OrderItem orderItem = new OrderItem(
            1L, 100L, 200L,
            "테스트 상품", "사이즈-M", new BigDecimal("10000"), 2
        );

        // when & then
        assertThatThrownBy(orderItem::completeDelivery)
            .isInstanceOf(OrderException.class)
            .hasMessageContaining("유효하지 않은 주문 상품 상태 변경입니다");
    }

    @Test
    @DisplayName("취소 가능 여부 - PREPARING 상태")
    void canBeCancelledWhenPreparing() {
        // given
        OrderItem orderItem = new OrderItem(
            1L, 100L, 200L,
            "테스트 상품", "사이즈-M", new BigDecimal("10000"), 2
        );

        // when & then
        assertThat(orderItem.canBeCancelled()).isTrue();
    }

    @Test
    @DisplayName("취소 불가능 - SHIPPING 상태")
    void cannotBeCancelledWhenShipping() {
        // given
        OrderItem orderItem = new OrderItem(
            1L, 1L, 100L, 200L,
            "테스트 상품", "사이즈-M", new BigDecimal("10000"),
            2, new BigDecimal("20000"), OrderItemStatus.SHIPPING
        );

        // when & then
        assertThat(orderItem.canBeCancelled()).isFalse();
    }

    @Test
    @DisplayName("배송 시작 가능 여부 - PREPARING 상태")
    void canStartShippingWhenPreparing() {
        // given
        OrderItem orderItem = new OrderItem(
            1L, 100L, 200L,
            "테스트 상품", "사이즈-M", new BigDecimal("10000"), 2
        );

        // when & then
        assertThat(orderItem.canStartShipping()).isTrue();
    }

    @Test
    @DisplayName("배송 시작 불가능 - SHIPPING 상태")
    void cannotStartShippingWhenShipping() {
        // given
        OrderItem orderItem = new OrderItem(
            1L, 1L, 100L, 200L,
            "테스트 상품", "사이즈-M", new BigDecimal("10000"),
            2, new BigDecimal("20000"), OrderItemStatus.SHIPPING
        );

        // when & then
        assertThat(orderItem.canStartShipping()).isFalse();
    }

    @Test
    @DisplayName("배송 완료 가능 여부 - SHIPPING 상태")
    void canCompleteDeliveryWhenShipping() {
        // given
        OrderItem orderItem = new OrderItem(
            1L, 1L, 100L, 200L,
            "테스트 상품", "사이즈-M", new BigDecimal("10000"),
            2, new BigDecimal("20000"), OrderItemStatus.SHIPPING
        );

        // when & then
        assertThat(orderItem.canCompleteDelivery()).isTrue();
    }

    @Test
    @DisplayName("배송 완료 불가능 - PREPARING 상태")
    void cannotCompleteDeliveryWhenPreparing() {
        // given
        OrderItem orderItem = new OrderItem(
            1L, 100L, 200L,
            "테스트 상품", "사이즈-M", new BigDecimal("10000"), 2
        );

        // when & then
        assertThat(orderItem.canCompleteDelivery()).isFalse();
    }

    @Test
    @DisplayName("전체 상태 전환 플로우 - PREPARING -> SHIPPING -> DELIVERED")
    void fullStatusTransitionFlow() {
        // given
        OrderItem orderItem = new OrderItem(
            1L, 100L, 200L,
            "테스트 상품", "사이즈-M", new BigDecimal("10000"), 2
        );

        // when - PREPARING -> SHIPPING
        OrderItem shippingItem = orderItem.startShipping();
        assertThat(shippingItem.getItemStatus()).isEqualTo(OrderItemStatus.SHIPPING);

        // when - SHIPPING -> DELIVERED
        OrderItem deliveredItem = shippingItem.completeDelivery();
        assertThat(deliveredItem.getItemStatus()).isEqualTo(OrderItemStatus.DELIVERED);

        // then - 원본은 불변
        assertThat(orderItem.getItemStatus()).isEqualTo(OrderItemStatus.PREPARING);
        assertThat(shippingItem.getItemStatus()).isEqualTo(OrderItemStatus.SHIPPING);
    }
}
