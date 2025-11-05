package hhplus.ecommerce.point.application.service;

import hhplus.ecommerce.point.presentation.dto.response.PointTransactionResponse;
import org.springframework.stereotype.Component;

import hhplus.ecommerce.point.domain.model.PointHistory;
import hhplus.ecommerce.point.presentation.dto.response.PointHistoryResponse;
import hhplus.ecommerce.point.presentation.dto.response.PointTransactionResponse;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Point 관련 Entity와 DTO 간 변환 담당
 */
@Component
public class PointMapper {

    public PointTransactionResponse toTransactionResponse(PointHistory history) {
        return new PointTransactionResponse(
                history.getPointHistoryId(),
                history.getUserId(),
                history.getTransactionType(),
                history.getAmount(),
                history.getBalanceAfter(),
                history.getDescription(),
                history.getCreatedAt()
        );
    }

    public PointHistoryResponse toHistoryResponse(PointHistory history) {
        return new PointHistoryResponse(
                history.getPointHistoryId(),
                history.getUserId(),
                history.getAmount(),
                history.getBalanceAfter(),
                history.getTransactionType(),
                history.getDescription(),
                history.getOrderId(),
                history.getCreatedAt()
        );
    }

    public List<PointHistoryResponse> toHistoryResponseList(List<PointHistory> histories) {
        return histories.stream()
                .map(this::toHistoryResponse)
                .collect(Collectors.toList());
    }
}

