package hhplus.ecommerce.unitTest.product.application;

import hhplus.ecommerce.common.domain.exception.ProductException;
import hhplus.ecommerce.common.domain.exception.StockException;
import hhplus.ecommerce.product.application.service.StockService;
import hhplus.ecommerce.product.domain.model.ProductOption;
import hhplus.ecommerce.product.domain.model.ReservationStatus;
import hhplus.ecommerce.product.domain.model.StockHistory;
import hhplus.ecommerce.product.domain.model.StockReservation;
import hhplus.ecommerce.product.domain.repository.ProductOptionRepository;
import hhplus.ecommerce.product.domain.repository.StockHistoryRepository;
import hhplus.ecommerce.product.domain.repository.StockReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private ProductOptionRepository productOptionRepository;

    @Mock
    private StockReservationRepository stockReservationRepository;

    @Mock
    private StockHistoryRepository stockHistoryRepository;

    @InjectMocks
    private StockService stockService;

    private ProductOption testProductOption;

    @BeforeEach
    void setUp() {
        testProductOption = ProductOption.create(
                1L,
                "테스트 옵션",
                BigDecimal.valueOf(1000),
                100,
                true
        );
    }

    @Test
    @DisplayName("재고를 조회할 수 있다")
    void getStock() {
        // given
        Long productOptionId = 1L;
        when(productOptionRepository.findById(productOptionId))
                .thenReturn(Optional.of(testProductOption));
        when(stockReservationRepository.findAllReservedByProductOptionId(productOptionId))
                .thenReturn(List.of());

        // when
        var result = stockService.getStock(productOptionId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getPhysicalQuantity()).isEqualTo(100);
        assertThat(result.getAvailableQuantity()).isEqualTo(100);
        verify(productOptionRepository, times(1)).findById(productOptionId);
    }

    @Test
    @DisplayName("재고를 증가시킬 수 있다")
    void updateStockIncrease() {
        // given
        Long productOptionId = 1L;
        int increaseAmount = 50;

        when(productOptionRepository.findById(productOptionId))
                .thenReturn(Optional.of(testProductOption));
        when(stockReservationRepository.findAllReservedByProductOptionId(productOptionId))
                .thenReturn(List.of());
        when(productOptionRepository.save(any(ProductOption.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(stockHistoryRepository.save(any(StockHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        StockHistory result = stockService.updateStock(productOptionId, increaseAmount, 1L, "재고 추가");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getAdjustmentType()).isNotNull();
        assertThat(result.getAdjustmentType().name()).isEqualTo("ADD");
        verify(productOptionRepository, times(1)).save(any(ProductOption.class));
        verify(stockHistoryRepository, times(1)).save(any(StockHistory.class));
    }

    @Test
    @DisplayName("재고를 감소시킬 수 있다")
    void updateStockDecrease() {
        // given
        Long productOptionId = 1L;
        int decreaseAmount = -30;

        when(productOptionRepository.findById(productOptionId))
                .thenReturn(Optional.of(testProductOption));
        when(stockReservationRepository.findAllReservedByProductOptionId(productOptionId))
                .thenReturn(List.of());
        when(productOptionRepository.save(any(ProductOption.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(stockHistoryRepository.save(any(StockHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        StockHistory result = stockService.updateStock(productOptionId, decreaseAmount, 1L, "재고 차감");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getAdjustmentType()).isNotNull();
        assertThat(result.getAdjustmentType().name()).isEqualTo("USE");
        verify(productOptionRepository, times(1)).save(any(ProductOption.class));
        verify(stockHistoryRepository, times(1)).save(any(StockHistory.class));
    }

    @Test
    @DisplayName("0으로 재고 변경 시도 시 예외가 발생한다")
    void updateStockZero() {
        // given
        Long productOptionId = 1L;
        when(productOptionRepository.findById(productOptionId))
                .thenReturn(Optional.of(testProductOption));

        // when & then
        assertThatThrownBy(() -> stockService.updateStock(productOptionId, 0, 1L, "재고 변경"))
                .isInstanceOf(StockException.class);
    }

    @Test
    @DisplayName("재고를 예약할 수 있다")
    void reserveStock() {
        // given
        Long orderId = 1L;
        Long productOptionId = 1L;
        int quantity = 10;

        when(productOptionRepository.findById(productOptionId))
                .thenReturn(Optional.of(testProductOption));
        when(stockReservationRepository.findAllReservedByProductOptionId(productOptionId))
                .thenReturn(List.of());
        when(stockReservationRepository.save(any(StockReservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        StockReservation result = stockService.reserveStock(orderId, productOptionId, quantity);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(orderId);
        assertThat(result.getReservedQuantity()).isEqualTo(quantity);
        verify(stockReservationRepository, times(1)).save(any(StockReservation.class));
    }

    @Test
    @DisplayName("재고가 부족하면 예약 시 예외가 발생한다")
    void reserveStockInsufficient() {
        // given
        Long orderId = 1L;
        Long productOptionId = 1L;
        int excessiveQuantity = 150;

        when(productOptionRepository.findById(productOptionId))
                .thenReturn(Optional.of(testProductOption));
        when(stockReservationRepository.findAllReservedByProductOptionId(productOptionId))
                .thenReturn(List.of());

        // when & then
        assertThatThrownBy(() -> stockService.reserveStock(orderId, productOptionId, excessiveQuantity))
                .isInstanceOf(StockException.class);

        verify(stockReservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("재고 예약을 확정할 수 있다")
    void confirmStockReservation() {
        // given
        Long reservationId = 1L;
        StockReservation reservation = StockReservation.create(1L, 1L, 10);

        when(stockReservationRepository.findByStockReservationId(reservationId))
                .thenReturn(Optional.of(reservation));
        when(productOptionRepository.findById(1L))
                .thenReturn(Optional.of(testProductOption));
        when(productOptionRepository.save(any(ProductOption.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(stockReservationRepository.save(any(StockReservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        StockReservation result = stockService.confirmStockReservation(reservationId);

        // then
        assertThat(result.isConfirmed()).isTrue();
        verify(productOptionRepository, times(1)).save(any(ProductOption.class));
        verify(stockReservationRepository, times(1)).save(any(StockReservation.class));
    }

    @Test
    @DisplayName("재고 예약을 해제할 수 있다")
    void releaseStockReservation() {
        // given
        Long reservationId = 1L;
        StockReservation reservation = StockReservation.create(1L, 1L, 10);

        when(stockReservationRepository.findByStockReservationId(reservationId))
                .thenReturn(Optional.of(reservation));
        when(stockReservationRepository.save(any(StockReservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        StockReservation result = stockService.releaseStockReservation(reservationId);

        // then
        assertThat(result.getReservationStatus()).isEqualTo(ReservationStatus.RELEASED);
        verify(stockReservationRepository, times(1)).save(any(StockReservation.class));
    }

    @Test
    @DisplayName("확정된 예약은 해제할 수 없다")
    void releaseConfirmedReservation() {
        // given
        Long reservationId = 1L;
        StockReservation reservation = StockReservation.create(1L, 1L, 10);
        reservation.confirm();

        when(stockReservationRepository.findByStockReservationId(reservationId))
                .thenReturn(Optional.of(reservation));

        // when & then
        assertThatThrownBy(() -> stockService.releaseStockReservation(reservationId))
                .isInstanceOf(StockException.class);

        verify(stockReservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("만료된 재고 예약 목록을 조회할 수 있다")
    void getExpiredReservations() {
        // given
        List<StockReservation> reservations = List.of(
                StockReservation.create(1L, 1L, 10)
        );

        when(stockReservationRepository.findAllByReservationStatus(ReservationStatus.RESERVED))
                .thenReturn(reservations);

        // when
        List<StockReservation> result = stockService.getExpiredReservations();

        // then
        assertThat(result).isNotNull();
        verify(stockReservationRepository, times(1))
                .findAllByReservationStatus(ReservationStatus.RESERVED);
    }
}
