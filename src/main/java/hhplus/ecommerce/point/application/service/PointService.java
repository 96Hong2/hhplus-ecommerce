package hhplus.ecommerce.point.application.service;

import hhplus.ecommerce.common.domain.constants.BusinessConstants;
import hhplus.ecommerce.common.domain.exception.PointException;
import hhplus.ecommerce.point.domain.model.PointHistory;
import hhplus.ecommerce.point.domain.model.TransactionType;
import hhplus.ecommerce.point.domain.repository.PointHistoryRepository;
import hhplus.ecommerce.user.domain.model.User;
import hhplus.ecommerce.user.domain.repository.UserRepository;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 포인트 서비스
 *
 * 낙관적 락(Optimistic Lock)을 사용하여 동시성 제어
 * - User 엔티티의 @Version 필드를 통해 JPA가 자동으로 버전 체크
 * - 충돌 발생 시 재시도 로직으로 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PointService {

    private final PointHistoryRepository pointHistoryRepository;
    private final UserRepository userRepository;

    private static final int MAX_RETRY_COUNT = 10; // 최대 재시도 횟수 (높은 동시성 환경 대응)

    /**
     * 유저의 포인트를 충전한다.
     *
     * 낙관적 락 방식:
     * - 재시도 로직을 통해 OptimisticLockException 처리
     * - 최대 3회까지 재시도
     *
     * @param userId 사용자 ID
     * @param amount 충전할 금액
     * @param description 충전 내역 설명
     * @return 충전 후 포인트 이력
     */
    public PointHistory chargePoint(Long userId, BigDecimal amount, String description) {
        // 최소 충전금액 검증
        if (amount.compareTo(BusinessConstants.MIN_CHARGE_AMOUNT) < 0) {
            throw PointException.invalidPointAmount(amount);
        }

        // 재시도 로직
        int retryCount = 0;
        while (retryCount < MAX_RETRY_COUNT) {
            try {
                return chargePointInternal(userId, amount, description);
            } catch (OptimisticLockException | ObjectOptimisticLockingFailureException e) {
                retryCount++;
                log.warn("포인트 충전 중 낙관적 락 충돌 발생. userId={}, retryCount={}/{}",
                         userId, retryCount, MAX_RETRY_COUNT);

                if (retryCount >= MAX_RETRY_COUNT) {
                    log.error("포인트 충전 실패: 최대 재시도 횟수 초과. userId={}", userId);
                    throw PointException.chargeFailed(userId, "동시 요청이 많아 처리할 수 없습니다. 잠시 후 다시 시도해주세요.");
                }

                // 재시도 전 짧은 대기 (충돌 완화 - Exponential Backoff)
                try {
                    Thread.sleep(10 * retryCount); // 10ms, 20ms, 30ms, ...
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw PointException.chargeFailed(userId, "포인트 충전 중 인터럽트 발생");
                }
            }
        }

        throw PointException.chargeFailed(userId, "포인트 충전 실패");
    }

    /**
     * 포인트 충전 내부 로직 (낙관적 락 적용)
     */
    @Transactional
    protected PointHistory chargePointInternal(Long userId, BigDecimal amount, String description) {
        // 사용자 조회 (낙관적 락 - 별도 락 불필요)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> PointException.chargeFailed(userId, "사용자를 찾을 수 없습니다."));

        // 포인트 충전
        BigDecimal newBalance = user.getPointBalance().add(amount);
        user.chargePoint(amount);
        userRepository.save(user); // JPA가 version을 자동으로 증가시키고 체크

        // 포인트 충전 내역 저장
        PointHistory history = new PointHistory(null, userId, amount, newBalance, description);
        return pointHistoryRepository.save(history);
    }

    /**
     * 유저의 포인트를 사용한다.
     *
     * 낙관적 락 방식:
     * - 재시도 로직을 통해 OptimisticLockException 처리
     * - 최대 3회까지 재시도
     *
     * @param userId 사용자 ID
     * @param amount 사용할 금액
     * @param orderId 주문 ID
     * @param description 사용 내역 설명
     * @return 포인트 사용 내역
     */
    public PointHistory usePoint(Long userId, BigDecimal amount, Long orderId, String description) {
        // 사용금액 검증
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw PointException.invalidPointAmount(amount);
        }

        // 재시도 로직
        int retryCount = 0;
        while (retryCount < MAX_RETRY_COUNT) {
            try {
                return usePointInternal(userId, amount, orderId, description);
            } catch (OptimisticLockException | ObjectOptimisticLockingFailureException e) {
                retryCount++;
                log.warn("포인트 사용 중 낙관적 락 충돌 발생. userId={}, retryCount={}/{}",
                         userId, retryCount, MAX_RETRY_COUNT);

                if (retryCount >= MAX_RETRY_COUNT) {
                    log.error("포인트 사용 실패: 최대 재시도 횟수 초과. userId={}", userId);
                    throw PointException.useFailed(userId, "동시 요청이 많아 처리할 수 없습니다. 잠시 후 다시 시도해주세요.");
                }

                // 재시도 전 짧은 대기 (충돌 완화 - Exponential Backoff)
                try {
                    Thread.sleep(10 * retryCount); // 10ms, 20ms, 30ms, ...
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw PointException.useFailed(userId, "포인트 사용 중 인터럽트 발생");
                }
            }
        }

        throw PointException.useFailed(userId, "포인트 사용 실패");
    }

    /**
     * 포인트 사용 내부 로직 (낙관적 락 적용)
     */
    @Transactional
    protected PointHistory usePointInternal(Long userId, BigDecimal amount, Long orderId, String description) {
        // 사용자 조회 (낙관적 락 - 별도 락 불필요)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> PointException.useFailed(userId, "사용자를 찾을 수 없습니다."));

        // 사용 가능 여부 검증
        BigDecimal balance = user.getPointBalance();
        if (balance.compareTo(amount) < 0) {
            throw PointException.insufficientPointBalance(userId, amount, balance);
        }

        // 포인트 사용
        user.usePoint(amount);
        userRepository.save(user); // JPA가 version을 자동으로 증가시키고 체크

        // 포인트 사용 내역 저장
        PointHistory history = new PointHistory(null, userId, amount, balance.subtract(amount), orderId, description);
        return pointHistoryRepository.save(history);
    }

    /**
     * 유저의 포인트 히스토리를 조회한다.
     * @param userId
     * @param transactionType
     * @return 유저의 포인트 거래 내역
     */
    public List<PointHistory> getPointHistory(Long userId, TransactionType transactionType) {
        if (transactionType != null) {
            return pointHistoryRepository.findByUserIdAndType(userId, transactionType);
        }
        return pointHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
