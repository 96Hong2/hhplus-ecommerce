package hhplus.ecommerce.point.presentation.controller;

import hhplus.ecommerce.point.application.service.PointMapper;
import hhplus.ecommerce.point.application.service.PointService;
import hhplus.ecommerce.point.presentation.dto.request.PointChargeRequest;
import hhplus.ecommerce.point.presentation.dto.request.PointUseRequest;
import hhplus.ecommerce.point.presentation.dto.response.PointHistoryResponse;
import hhplus.ecommerce.point.presentation.dto.response.PointTransactionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import hhplus.ecommerce.common.presentation.response.ApiResponse;
import hhplus.ecommerce.point.application.service.PointMapper;
import hhplus.ecommerce.point.application.service.PointService;
import hhplus.ecommerce.point.domain.model.PointHistory;
import hhplus.ecommerce.point.domain.model.TransactionType;
import hhplus.ecommerce.point.presentation.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 포인트 충전/사용/히스토리 조회 HTTP 요청 처리 컨트롤러
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/point")
public class PointController {

    private final PointService pointService;
    private final PointMapper pointMapper;

    /**
     * 포인트 충전
     * POST /api/point/charge/{userId}
     */
    @PostMapping("/charge/{userId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PointTransactionResponse> chargePoint(
            @PathVariable Long userId,
            @Valid @RequestBody PointChargeRequest request) {

        PointHistory history = pointService.chargePoint(
                userId,
                request.getAmount(),
                request.getDescription()
        );

        PointTransactionResponse response = pointMapper.toTransactionResponse(history);
        return ApiResponse.success(response, "포인트가 충전되었습니다.");
    }

    /**
     * 포인트 사용
     * POST /api/point/use/{userId}
     */
    @PostMapping("/use/{userId}")
    public ApiResponse<PointTransactionResponse> usePoint(
            @PathVariable Long userId,
            @Valid @RequestBody PointUseRequest request) {

        PointHistory history = pointService.usePoint(
                userId,
                request.getAmount(),
                request.getOrderId(),
                request.getDescription()
        );

        PointTransactionResponse response = pointMapper.toTransactionResponse(history);
        return ApiResponse.success(response, "포인트가 사용되었습니다.");
    }

    /**
     * 포인트 히스토리 조회
     * GET /api/point/{userId}?transactionType=CHARGE
     */
    @GetMapping("/{userId}")
    public ApiResponse<List<PointHistoryResponse>> getPointHistory(
            @PathVariable Long userId,
            @RequestParam(required = false) TransactionType transactionType) {

        List<PointHistory> histories = pointService.getPointHistory(userId, transactionType);
        List<PointHistoryResponse> responses = pointMapper.toHistoryResponseList(histories);

        return ApiResponse.success(responses);
    }
}

