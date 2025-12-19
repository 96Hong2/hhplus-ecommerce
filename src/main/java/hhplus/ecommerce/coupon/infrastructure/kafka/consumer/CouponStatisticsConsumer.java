package hhplus.ecommerce.coupon.infrastructure.kafka.consumer;

import hhplus.ecommerce.coupon.domain.event.CouponIssuedEvent;
import hhplus.ecommerce.coupon.domain.event.CouponUsedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import static hhplus.ecommerce.coupon.infrastructure.kafka.config.CouponKafkaConfig.COUPON_ISSUED_TOPIC;
import static hhplus.ecommerce.coupon.infrastructure.kafka.config.CouponKafkaConfig.COUPON_USED_TOPIC;

/**
 * 쿠폰 통계 처리 Consumer
 * 발급 및 사용 통계를 실시간으로 집계
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CouponStatisticsConsumer {

    @KafkaListener(
            topics = COUPON_ISSUED_TOPIC,
            groupId = "coupon-statistics-group"
    )
    public void consumeCouponIssued(@Payload CouponIssuedEvent event, Acknowledgment ack) {
        try {
            log.info("[통계] 쿠폰 발급 집계 - couponId={}, userId={}",
                    event.getCouponId(), event.getUserId());

            // 향후 실제 통계 서비스 연동 지점
            // statisticsService.updateIssuedCount(event.getCouponId());

            ack.acknowledge();

        } catch (Exception e) {
            log.error("쿠폰 발급 통계 처리 실패. couponId={}", event.getCouponId(), e);
        }
    }

    @KafkaListener(
            topics = COUPON_USED_TOPIC,
            groupId = "coupon-statistics-group"
    )
    public void consumeCouponUsed(@Payload CouponUsedEvent event, Acknowledgment ack) {
        try {
            log.info("[통계] 쿠폰 사용 집계 - couponId={}, orderId={}",
                    event.getCouponId(), event.getOrderId());

            // 통계 서비스 연동
            // statisticsService.updateUsedCount(event.getCouponId());

            ack.acknowledge();

        } catch (Exception e) {
            log.error("쿠폰 사용 통계 처리 실패. couponId={}", event.getCouponId(), e);
        }
    }
}
