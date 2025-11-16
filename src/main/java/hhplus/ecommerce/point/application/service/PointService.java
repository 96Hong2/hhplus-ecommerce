package hhplus.ecommerce.point.application.service;

import hhplus.ecommerce.common.domain.constants.BusinessConstants;
import hhplus.ecommerce.common.domain.exception.PointException;
import hhplus.ecommerce.point.domain.model.PointHistory;
import hhplus.ecommerce.point.domain.model.TransactionType;
import hhplus.ecommerce.point.domain.repository.PointHistoryRepository;
import hhplus.ecommerce.user.domain.model.User;
import hhplus.ecommerce.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
public class PointService {

    private final PointHistoryRepository pointHistoryRepository;
    private final UserRepository userRepository;

    // 사용자별 락 관리 (동시성 제어)
    ConcurrentHashMap<Long, ReentrantLock> userLockMap = new ConcurrentHashMap<>();

    /**
     * 유저의 포인트를 충전한다.
     * @param userId
     * @param amount
     * @param description
     * @return 충전 후 포인트 이력
     *
     * @Transactional: User 잔액 업데이트 + PointHistory 저장이 원자적으로 처리되어야 함
     */
    @Transactional
    public PointHistory chargePoint(Long userId, BigDecimal amount, String description) {

        // 최소 충전금액 검증
        if (amount.compareTo(BusinessConstants.MIN_CHARGE_AMOUNT) < 0) {
            throw PointException.invalidPointAmount(amount);
        }

        ReentrantLock lock = userLockMap.computeIfAbsent(userId, k -> new ReentrantLock());
        lock.lock();

        try {
            // 사용자 조회
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> PointException.chargeFailed(userId, "사용자를 찾을 수 없습니다."));

            // 포인트 충전
            BigDecimal newBalance = user.getPointBalance().add(amount);
            user.chargePoint(amount);
            userRepository.save(user);

            // 포인트 충전 내역 저장
            PointHistory history = new PointHistory(null, userId, amount, newBalance, description);
            return pointHistoryRepository.save(history);

        } catch (Exception e) {
            throw PointException.chargeFailed(userId, e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    /**
     * 유저의 포인트를 사용한다.
     * @param userId
     * @param amount
     * @param orderId
     * @param description
     * @return 포인트 사용 내역
     *
     * @Transactional: User 잔액 차감 + PointHistory 저장이 원자적으로 처리되어야 함
     */
    @Transactional
    public PointHistory usePoint(Long userId, BigDecimal amount, Long orderId, String description) {
        ReentrantLock lock = userLockMap.computeIfAbsent(userId, k -> new ReentrantLock());
        lock.lock();

        // 사용금액 검증
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw PointException.invalidPointAmount(amount);
        }

        try {
            // 사용자 조회
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> PointException.useFailed(userId, "사용자를 찾을 수 없습니다."));

            // 사용 가능 여부 검증
            BigDecimal balance = user.getPointBalance();
            if (balance.compareTo(amount) < 0) {
                throw PointException.insufficientPointBalance(userId, amount, balance);
            }

            // 포인트 사용
            user.usePoint(amount);
            userRepository.save(user);

            // 포인트 사용 내역 저장
            PointHistory history = new PointHistory(null, userId, amount, balance.subtract(amount), orderId, description);
            return pointHistoryRepository.save(history);

        } catch (Exception e) {
            throw PointException.useFailed(userId, e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    /**
     * 유저의 포인트 히스토리를 조회한다.
     * @param userId
     * @param transactionType
     * @return 유저의 포인트 거래 내역
     */
    public List<PointHistory> getPointHistory(Long userId, TransactionType transactionType) {
        if (transactionType != null) {
            return pointHistoryRepository.findByUserIdAndTransactionType(userId, transactionType);
        }
        return pointHistoryRepository.findByUserId(userId);
    }
}
