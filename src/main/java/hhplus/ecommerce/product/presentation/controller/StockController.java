package hhplus.ecommerce.product.presentation.controller;

import hhplus.ecommerce.product.application.service.StockMapper;
import hhplus.ecommerce.product.application.service.StockService;
import hhplus.ecommerce.product.presentation.dto.request.StockChangeRequest;
import hhplus.ecommerce.product.presentation.dto.request.StockReservationRequest;
import hhplus.ecommerce.product.presentation.dto.response.StockHistoryResponse;
import hhplus.ecommerce.product.presentation.dto.response.StockReservationResponse;
import hhplus.ecommerce.product.presentation.dto.response.StockResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;
    private final StockMapper stockMapper;

    /**
     * 상품 재고 조회
     * GET /api/stock/{productOptionId}
     * @param productOptionId 상품 옵션 ID
     * @return 재고 정보 (물리적 재고, 예약된 재고, 판매 가능 재고)
     */
    @GetMapping("/{productOptionId}")
    public StockResponse getStock(@PathVariable Long productOptionId) {
        return stockService.getStock(productOptionId);
    }

    /**
     * 상품 재고 변경
     * POST /api/stock/{productOptionId}
     * @param productOptionId 상품 옵션 ID
     * @param request 재고 변경 요청 (amount: 양수(추가), 음수(감소), updatedBy: 수정자 ID)
     * @return 변경된 재고 정보
     */
    @PostMapping("/{productOptionId}")
    public StockHistoryResponse updateStock(
            @PathVariable Long productOptionId,
            @Valid @RequestBody StockChangeRequest request) {
        return stockMapper.toStockHistoryResponse(
                stockService.updateStock(
                    productOptionId,
                    request.getAmount(),
                    request.getUpdatedBy(),
                    request.getDescription()
        ));
    }

    /**
     * 재고 예약 (주문 생성 시 호출)
     * POST /api/stock/reserve
     * @param request 재고 예약 요청 (orderId, productOptionId, quantity)
     * @return 재고 예약 정보 (15분간 유효)
     */
    @PostMapping("/reserve")
    public StockReservationResponse reserveStock(@Valid @RequestBody StockReservationRequest request) {
        return stockMapper.toStockReservationResponse(
                stockService.reserveStock(
                        request.getOrderId(),
                        request.getProductOptionId(),
                        request.getReservedQuantity()
                )
        );
    }

    /**
     * 재고 예약 확정 (결제 완료 시 호출)
     * POST /api/stock/reserve/{reservationId}/confirm
     * @param reservationId 재고 예약 ID
     * @return 확정된 재고 예약 정보
     */
    @PostMapping("/reserve/{reservationId}/confirm")
    public StockReservationResponse confirmStockReservation(@PathVariable Long reservationId) {
        return stockMapper.toStockReservationResponse(
                stockService.confirmStockReservation(reservationId)
        );
    }

    /**
     * 재고 예약 해제 (주문 취소 또는 타임아웃 시 호출)
     * POST /api/stock/reserve/{reservationId}/release
     * @param reservationId 재고 예약 ID
     * @return 해제된 재고 예약 정보
     */
    @PostMapping("/reserve/{reservationId}/release")
    public StockReservationResponse releaseStockReservation(@PathVariable Long reservationId) {
        return stockMapper.toStockReservationResponse(
                stockService.releaseStockReservation(reservationId)
        );
    }

    /**
     * 만료된 재고 예약 목록 조회 (배치 처리용)
     * GET /api/stock/reserve/expired
     * @return 타임아웃이 지난 RESERVED 상태의 예약 목록
     */
    @GetMapping("/reserve/expired")
    public List<StockReservationResponse> getExpiredReservations() {
        return stockService.getExpiredReservations().stream()
                .map(stockMapper::toStockReservationResponse)
                .toList();
    }
}
