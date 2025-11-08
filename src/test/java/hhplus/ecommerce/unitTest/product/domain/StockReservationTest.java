package hhplus.ecommerce.unitTest.product.domain;

import hhplus.ecommerce.common.domain.exception.StockException;
import hhplus.ecommerce.product.domain.model.ReservationStatus;
import hhplus.ecommerce.product.domain.model.StockReservation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class StockReservationTest {

    @Test
    @DisplayName("재고 예약을 정상적으로 생성할 수 있다.")
    void createStockReservation() {
        // when
        StockReservation reservation = StockReservation.create(1L, 100L, 5);

        // then
        assertThat(reservation.getProductOptionId()).isEqualTo(1L);
        assertThat(reservation.getOrderId()).isEqualTo(100L);
        assertThat(reservation.getReservedQuantity()).isEqualTo(5);
        assertThat(reservation.getReservationStatus()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(reservation.getReservedAt()).isNotNull();
        assertThat(reservation.getExpiresAt()).isNotNull();
    }

    @Test
    @DisplayName("재고 예약 생성 시 productOptionId가 null이면 예외가 발생한다.")
    void createStockReservationWithNullProductOptionId() {
        // when & then
        assertThatThrownBy(() -> StockReservation.create(null, 100L, 5))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("재고 예약 생성 시 orderId가 null이면 예외가 발생한다.")
    void createStockReservationWithNullOrderId() {
        // when & then
        assertThatThrownBy(() -> StockReservation.create(1L, null, 5))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("재고 예약 생성 시 수량이 0 이하면 예외가 발생한다.")
    void createStockReservationWithInvalidQuantity() {
        // when & then
        assertThatThrownBy(() -> StockReservation.create(1L, 100L, 0))
                .isInstanceOf(StockException.class);

        assertThatThrownBy(() -> StockReservation.create(1L, 100L, -5))
                .isInstanceOf(StockException.class);
    }

    @Test
    @DisplayName("재고 예약 생성 시 만료 시간이 과거면 예외가 발생한다.")
    void createStockReservationWithPastExpiration() {
        // when & then
        LocalDateTime pastTime = LocalDateTime.now().minusMinutes(10);
        assertThatThrownBy(() -> StockReservation.create(1L, 100L, 5, pastTime))
                .isInstanceOf(StockException.class);
    }

    @Test
    @DisplayName("재고 예약을 확정할 수 있다.")
    void confirmReservation() {
        // given
        LocalDateTime futureTime = LocalDateTime.now().plusMinutes(10);
        StockReservation reservation = StockReservation.create(1L, 100L, 5, futureTime);

        // when
        StockReservation confirmed = reservation.confirm();

        // then
        assertThat(confirmed.getReservationStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(confirmed.isConfirmed()).isTrue();
    }

    @Test
    @DisplayName("만료된 예약은 확정할 수 없다.")
    void confirmExpiredReservation() {
        // given - 1초 후 만료되는 예약
        LocalDateTime nearFutureTime = LocalDateTime.now().plusSeconds(1);
        StockReservation reservation = StockReservation.create(1L, 100L, 5, nearFutureTime);

        // 2초 대기하여 만료시킴
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // when & then
        assertThatThrownBy(reservation::confirm)
                .isInstanceOf(StockException.class);
    }

    @Test
    @DisplayName("재고 예약을 해제할 수 있다.")
    void releaseReservation() {
        // given
        StockReservation reservation = StockReservation.create(1L, 100L, 5);

        // when
        StockReservation released = reservation.release();

        // then
        assertThat(released.getReservationStatus()).isEqualTo(ReservationStatus.RELEASED);
    }

    @Test
    @DisplayName("예약이 만료되었는지 확인할 수 있다.")
    void isExpired() {
        // given - 1초 후 만료되는 예약
        LocalDateTime nearFutureTime = LocalDateTime.now().plusSeconds(1);
        StockReservation reservation = StockReservation.create(1L, 100L, 5, nearFutureTime);

        // when & then - 처음에는 만료되지 않음
        assertThat(reservation.isExpired()).isFalse();

        // 2초 대기하여 만료시킴
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 이제 만료됨
        assertThat(reservation.isExpired()).isTrue();
    }

    @Test
    @DisplayName("예약이 활성 상태인지 확인할 수 있다.")
    void isActive() {
        // given
        LocalDateTime futureTime = LocalDateTime.now().plusMinutes(10);
        StockReservation reservation = StockReservation.create(1L, 100L, 5, futureTime);

        // when & then - RESERVED 상태이고 만료되지 않았으면 활성 상태
        assertThat(reservation.isActive()).isTrue();

        // 확정된 경우 활성 상태 아님
        StockReservation confirmed = reservation.confirm();
        assertThat(confirmed.isActive()).isFalse();

        // 해제된 경우 활성 상태 아님
        StockReservation released = reservation.release();
        assertThat(released.isActive()).isFalse();
    }

    @Test
    @DisplayName("예약 상태를 변경할 수 있다.")
    void updateStatus() {
        // given
        StockReservation reservation = StockReservation.create(1L, 100L, 5);

        // when
        StockReservation confirmed = reservation.updateStatus(ReservationStatus.CONFIRMED);

        // then
        assertThat(confirmed.getReservationStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        // 기존 객체는 변경되지 않음 (불변 객체)
        assertThat(reservation.getReservationStatus()).isEqualTo(ReservationStatus.RESERVED);
    }

    @Test
    @DisplayName("예약 상태 변경 시 null이면 예외가 발생한다.")
    void updateStatusWithNull() {
        // given
        StockReservation reservation = StockReservation.create(1L, 100L, 5);

        // when & then
        assertThatThrownBy(() -> reservation.updateStatus(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("예약 상태는 필수입니다.");
    }
}
