package hhplus.ecommerce.coupon.infrastructure.kafka.consumer;

import hhplus.ecommerce.coupon.domain.event.CouponIssuedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import static hhplus.ecommerce.coupon.infrastructure.kafka.config.CouponKafkaConfig.COUPON_ISSUED_TOPIC;

/**
 * 쿠폰 발급 알림 처리 Consumer
 *
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CouponNotificationConsumer {

    @KafkaListener(
            topics = COUPON_ISSUED_TOPIC,
            groupId = "coupon-notification-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeCouponIssued(CouponIssuedEvent event, Acknowledgment ack) {
        try {
            log.info("[알림] 쿠폰 발급 완료 - userId={}, couponName={}",
                    event.getUserId(), event.getCouponName());

            // 알림 서비스 연동
            // notificationService.sendCouponIssuedNotification(event);

            ack.acknowledge(); // 수동 커밋

        } catch (Exception e) {
            log.error("쿠폰 발급 알림 처리 실패. userId={}", event.getUserId(), e);
        }
    }
}
