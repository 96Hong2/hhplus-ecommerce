package hhplus.ecommerce.product.application.service;

import hhplus.ecommerce.common.domain.exception.ProductException;
import hhplus.ecommerce.common.domain.exception.StockException;
import hhplus.ecommerce.product.domain.model.ProductOption;
import hhplus.ecommerce.product.domain.model.ReservationStatus;
import hhplus.ecommerce.product.domain.model.StockHistory;
import hhplus.ecommerce.product.domain.model.StockReservation;
import hhplus.ecommerce.product.domain.repository.ProductOptionRepository;
import hhplus.ecommerce.product.domain.repository.StockHistoryRepository;
import hhplus.ecommerce.product.domain.repository.StockReservationRepository;
import hhplus.ecommerce.product.presentation.dto.response.StockResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockService {

    private final ProductOptionRepository productOptionRepository;
    private final StockReservationRepository stockReservationRepository;
    private final StockHistoryRepository stockHistoryRepository;

    /**
     * 상품 재고 조회
     * @param productOptionId 상품 옵션 ID
     * @return 재고 정보 (물리적 재고, 예약된 재고, 판매 가능 재고, 품절 여부)
     */
    public StockResponse getStock(Long productOptionId) {
        ProductOption productOption = productOptionRepository.findById(productOptionId)
                .orElseThrow(() -> ProductException.productOptionNotFound(productOptionId));

        // RESERVED 상태인 예약 재고만 조회 (확정/해제된 예약은 제외)
        List<StockReservation> stockReservationList = stockReservationRepository.findAllReservedByProductOptionId(productOptionId);

        int physicalQuantity = productOption.getStockQuantity();
        int reservedQuantity = stockReservationList.stream()
                .map(StockReservation::getReservedQuantity)
                .reduce(0, Integer::sum);

        int availableQuantity = physicalQuantity - reservedQuantity;

        if (availableQuantity < 0) {
            // 예약된 재고량이 실제 재고량보다 많습니다. [실제 재고량 : %d, 예약 재고량 : %d]
            throw StockException.stockDataInconsistency(physicalQuantity, reservedQuantity);
        }

        boolean isSoldOut = (availableQuantity == 0);

        return new StockResponse(
                productOptionId, physicalQuantity, reservedQuantity, availableQuantity, isSoldOut
        );
    }

    /**
     * 상품 재고 변경
     * @param productOptionId 상품 옵션 ID
     * @param amount 변경할 재고 수량 (양수: 추가, 음수: 감소)
     * @param updatedBy 수정자 ID
     * @param description 설명
     * @return 변경된 재고 정보
     */
    public StockHistory updateStock(Long productOptionId, int amount, Long updatedBy, String description) {
        ProductOption productOption = productOptionRepository.findById(productOptionId)
                .orElseThrow(() -> ProductException.productOptionNotFound(productOptionId));

        StockHistory stockHistory;

        if (amount == 0) {
            throw StockException.invalidStockAmount(amount);
        } else if (amount > 0) {
            // 재고 추가
            productOption.increaseStock(amount);
            stockHistory = StockHistory.forIncrease(productOptionId, amount, productOption.getStockQuantity(), description, updatedBy);
        } else {
            // 음수를 양수로 변환하여 처리
            int decreaseAmount = Math.abs(amount);

            StockResponse stockResponse = getStock(productOptionId);
            if (stockResponse.getAvailableQuantity() < decreaseAmount) {
                throw StockException.stockQuantityInsufficient(productOptionId, stockResponse.getAvailableQuantity(), decreaseAmount);
            }

            productOption.decreaseStock(decreaseAmount);
            stockHistory = StockHistory.forDecrease(productOptionId, decreaseAmount, productOption.getStockQuantity(), description, updatedBy);
        }

        productOptionRepository.save(productOption);
        return stockHistoryRepository.save(stockHistory);
    }

    /**
     * 재고 예약 (주문 생성 시 호출)
     * @param orderId 주문 ID
     * @param productOptionId 상품 옵션 ID
     * @param quantity 예약 수량
     * @return 재고 예약 정보 (15분간 유효)
     */
    public StockReservation reserveStock(Long orderId, Long productOptionId, int quantity) {
        // 상품옵션 존재 검증
        productOptionRepository.findById(productOptionId)
                .orElseThrow(() -> ProductException.productOptionNotFound(productOptionId));

        // 1. 재고 확인
        StockResponse stockResponse = getStock(productOptionId);
        if (stockResponse.getAvailableQuantity() < quantity) {
            throw StockException.stockQuantityInsufficient(productOptionId, stockResponse.getAvailableQuantity(), quantity);
        }

        // 2. 재고 예약 생성 및 저장 (15분 타임아웃)
        StockReservation reservation = StockReservation.create(productOptionId, orderId, quantity);
        return stockReservationRepository.save(reservation);
    }

    /**
     * 재고 예약 확정 (결제 완료 시 호출)
     * @param reservationId 재고 예약 ID
     * @return 확정된 재고 예약 정보
     */
    public StockReservation confirmStockReservation(Long reservationId) {
        // 1. 예약 정보 조회 및 상태 확인 (RESERVED 상태여야 함)
        StockReservation stockReservation = stockReservationRepository.findByStockReservationId(reservationId)
                .orElseThrow(() -> StockException.stockReservationNotFound(reservationId));

        // 2. 예약 상태를 CONFIRMED로 변경
        stockReservation.confirm();

        // 3. ProductOption의 physicalStock 감소
        ProductOption productOption = productOptionRepository.findById(stockReservation.getProductOptionId())
                .orElseThrow(() -> ProductException.productOptionNotFound(stockReservation.getProductOptionId()));
        productOption.decreaseStock(stockReservation.getReservedQuantity());
        productOptionRepository.save(productOption);

        return stockReservationRepository.save(stockReservation);
    }

    /**
     * 재고 예약 해제 (주문 취소 또는 타임아웃 시 호출)
     * @param reservationId 재고 예약 ID
     * @return 해제된 재고 예약 정보
     */
    public StockReservation releaseStockReservation(Long reservationId) {
        // 1. 예약 정보 조회
        StockReservation stockReservation = stockReservationRepository.findByStockReservationId(reservationId)
                .orElseThrow(() -> StockException.stockReservationNotFound(reservationId));

        if (stockReservation.isConfirmed()) {
            throw StockException.stockReservationAlreadyConfirmed(reservationId);
        }

        if (stockReservation.getReservationStatus().equals(ReservationStatus.RELEASED)) {
            throw StockException.stockReservationAlreadyReleased(reservationId);
        }

        // 2. 예약 상태를 RELEASED로 변경
        stockReservation.release();

        return stockReservationRepository.save(stockReservation);
    }

    /**
     * 만료된 재고 예약 목록 조회 (배치 처리용)
     * @return 타임아웃이 지난 RESERVED 상태의 예약 목록
     */
    public List<StockReservation> getExpiredReservations() {
        return stockReservationRepository.findAllByReservationStatus(ReservationStatus.RESERVED).stream()
                .filter(StockReservation::isExpired)
                .collect(Collectors.toList());
    }
}
