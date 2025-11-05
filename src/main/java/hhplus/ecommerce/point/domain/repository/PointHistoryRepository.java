package hhplus.ecommerce.point.domain.repository;

import hhplus.ecommerce.point.domain.model.PointHistory;
import hhplus.ecommerce.point.domain.model.TransactionType;

import java.util.List;

public interface PointHistoryRepository {

    /**
     * 포인트 거래 이력 저장
     * */
    PointHistory save(PointHistory pointHistory);

    /**
     * 유저의 포인트 히스토리를 조회한다.
     * */
    List<PointHistory> findByUserId(Long userId);

    /**
     * 유저의 거래 타입별 내역을 조회한다.
     * */
    List<PointHistory> findByUserIdAndTransactionType(Long userId, TransactionType transactionType);

    /**
     * 페이징하여 유저의 거래 내역을 조회한다.
     * */
    List<PointHistory> findByUserIdWithPaging(Long userId, int page, int size);

    /**
     * 페이징하여 유저의 거래 타입별 내역을 조회한다.
     * */
    List<PointHistory> findByUserIdAndTransactionTypeWithPaging(Long userId, TransactionType transactionType, int page, int size);

    /**
     * 유저의 포인트 거래 내역이 몇 개 있는지 조회한다.
     * */
    long countByUserId(Long userId);

    /**
     * 유저의 거래 타입별 내역이 몇 개 있는지 조회한다.
     * */
    long countByUserIdAndTransactionType(Long userId, TransactionType transactionType);
}

