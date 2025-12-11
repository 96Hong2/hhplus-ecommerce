package hhplus.ecommerce.product.application.scheduler;

import hhplus.ecommerce.common.domain.constants.BusinessConstants;
import hhplus.ecommerce.product.domain.model.PeriodType;
import hhplus.ecommerce.product.domain.model.PopularProduct;
import hhplus.ecommerce.product.domain.model.Product;
import hhplus.ecommerce.product.domain.repository.PopularProductRepository;
import hhplus.ecommerce.product.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 인기상품 랭킹 스케줄러
 *
 * 역할:
 * 1. Redis 키 초기화 (일별/주별/월별)
 * 2. PopularProduct 테이블 스냅샷 저장 (배치)
 * 3. DB-Redis 동기화 (장애 복구)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PopularProductScheduler {

    private final RedisTemplate<String, String> redisTemplate;
    private final PopularProductRepository popularProductRepository;
    private final ProductRepository productRepository;

    /**
     * 일별 랭킹 초기화 (매일 자정)
     */
    @Scheduled(cron = "0 0 0 * * ?") // 매일 00:00:00
    public void resetDailyRanking() {
        log.info("===== 일별 인기상품 랭킹 초기화 시작 =====");

        // 어제 Redis 데이터 스냅샷 저장
        Set<ZSetOperations.TypedTuple<String>> dailyRank = redisTemplate.opsForZSet().reverseRangeWithScores(BusinessConstants.REDIS_TOP_N_DAILY_KEY, 0, 99);

        if (dailyRank == null || dailyRank.isEmpty()) {
            log.info("dailyRank is empty. Skip snapshot creation. Date : " + LocalDate.now());
            return;
        };
        
        int rank = 1;
        for (ZSetOperations.TypedTuple<String> tuple : dailyRank) {
            Optional<Product> optionalProduct = productRepository.findById(Long.parseLong(Objects.requireNonNull(tuple.getValue())));
            
            if (optionalProduct.isEmpty()) continue;
            
            Product product = optionalProduct.get();
            PopularProduct snapshot = PopularProduct.create(
                product.getProductId(),
                tuple.getScore().intValue(), 
                LocalDate.now(), 
                PeriodType.DAILY,
                rank++);

            popularProductRepository.save(snapshot);
        }

        // 어제 키 삭제 (TTL은 안전장치로 둠)
        String backupKey = BusinessConstants.REDIS_TOP_N_DAILY_KEY + "_backup_" + LocalDate.now().toString().replace(":", "-");
        redisTemplate.rename(BusinessConstants.REDIS_TOP_N_DAILY_KEY, backupKey);
        redisTemplate.expire(backupKey, 12, TimeUnit.HOURS);

        log.info("===== 일별 인기상품 랭킹 초기화 완료 =====");
    }

    /**
     * 주별 랭킹 초기화 (매주 월요일 자정)
     */
    @Scheduled(cron = "0 0 0 ? * MON") // 매주 월요일 00:00:00
    public void resetWeeklyRanking() {
        log.info("===== 주별 인기상품 랭킹 초기화 시작 =====");

        // 지난주 Redis 데이터 스냅샷 저장
        Set<ZSetOperations.TypedTuple<String>> weeklyRank = redisTemplate.opsForZSet().reverseRangeWithScores(BusinessConstants.REDIS_TOP_N_WEEKLY_KEY, 0, 99);

        if (weeklyRank == null || weeklyRank.isEmpty()) {
            log.info("weeklyRank is empty. Skip snapshot creation. Date : " + LocalDate.now());
            return;
        }

        int rank = 1;
        for (ZSetOperations.TypedTuple<String> tuple : weeklyRank) {
            Optional<Product> optionalProduct = productRepository.findById(Long.parseLong(Objects.requireNonNull(tuple.getValue())));

            if (optionalProduct.isEmpty()) continue;

            Product product = optionalProduct.get();
            PopularProduct snapshot = PopularProduct.create(
                product.getProductId(),
                tuple.getScore().intValue(),
                LocalDate.now(),
                PeriodType.WEEKLY,
                rank++);

            popularProductRepository.save(snapshot);
        }

        // 지난주 키 삭제 (TTL은 안전장치로 둠)
        String backupKey = BusinessConstants.REDIS_TOP_N_WEEKLY_KEY + "_backup_" + LocalDate.now().toString().replace(":", "-");
        redisTemplate.rename(BusinessConstants.REDIS_TOP_N_WEEKLY_KEY, backupKey);
        redisTemplate.expire(backupKey, 12, TimeUnit.HOURS);

        log.info("===== 주별 인기상품 랭킹 초기화 완료 =====");
    }

    /**
     * 월별 랭킹 초기화 (매월 1일 자정)
     */
    @Scheduled(cron = "0 0 0 1 * ?") // 매월 1일 00:00:00
    public void resetMonthlyRanking() {
        log.info("===== 월별 인기상품 랭킹 초기화 시작 =====");

        // 지난달 Redis 데이터 스냅샷 저장
        Set<ZSetOperations.TypedTuple<String>> monthlyRank = redisTemplate.opsForZSet().reverseRangeWithScores(BusinessConstants.REDIS_TOP_N_MONTHLY_KEY, 0, 99);

        if (monthlyRank == null || monthlyRank.isEmpty()) {
            log.info("monthlyRank is empty. Skip snapshot creation. Date : " + LocalDate.now());
            return;
        }

        int rank = 1;
        for (ZSetOperations.TypedTuple<String> tuple : monthlyRank) {
            Optional<Product> optionalProduct = productRepository.findById(Long.parseLong(Objects.requireNonNull(tuple.getValue())));

            if (optionalProduct.isEmpty()) continue;

            Product product = optionalProduct.get();
            PopularProduct snapshot = PopularProduct.create(
                product.getProductId(),
                tuple.getScore().intValue(),
                LocalDate.now(),
                PeriodType.MONTHLY,
                rank++);

            popularProductRepository.save(snapshot);
        }

        // 지난달 키 삭제 (TTL은 안전장치로 둠)
        String backupKey = BusinessConstants.REDIS_TOP_N_MONTHLY_KEY + "_backup_" + LocalDate.now().toString().replace(":", "-");
        redisTemplate.rename(BusinessConstants.REDIS_TOP_N_MONTHLY_KEY, backupKey);
        redisTemplate.expire(backupKey, 12, TimeUnit.HOURS);

        log.info("===== 월별 인기상품 랭킹 초기화 완료 =====");
    }

    /**
     * 오래된 스냅샷 삭제 (매일 새벽 2시)
     * PopularProduct 테이블에서 90일 이전 데이터 삭제
     */
    @Scheduled(cron = "0 0 2 * * ?") // 매일 02:00:00
    public void cleanupOldSnapshots() {
        log.info("===== 오래된 스냅샷 데이터 삭제 시작 =====");

        LocalDate thresholdDate = LocalDate.now().minusDays(90);
        popularProductRepository.deleteByCalculationDateBefore(thresholdDate);

        log.info("===== 오래된 스냅샷 데이터 삭제 완료 =====");
    }
}
