package hhplus.ecommerce.coupon.infrastructure.kafka.producer;

import hhplus.ecommerce.coupon.domain.event.CouponIssuedEvent;
import hhplus.ecommerce.coupon.domain.event.CouponUsedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import static hhplus.ecommerce.coupon.infrastructure.kafka.config.CouponKafkaConfig.COUPON_ISSUED_TOPIC;
import static hhplus.ecommerce.coupon.infrastructure.kafka.config.CouponKafkaConfig.COUPON_USED_TOPIC;

/**
 * 쿠폰 이벤트를 Kafka로 발행하는 Producer
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CouponEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * 쿠폰 발급 이벤트를 Kafka로 전송
     * - 메시지 키는 userId로 설정하여 동일 사용자 이벤트 순서 보장
     */
    public void sendCouponIssuedEvent(CouponIssuedEvent event) {
        String key = event.getUserId().toString();

        kafkaTemplate.send(COUPON_ISSUED_TOPIC, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("쿠폰 발급 이벤트 전송 실패. userId={}, couponId={}",
                                event.getUserId(), event.getCouponId(), ex);
                    } else {
                        log.info("쿠폰 발급 이벤트 전송 성공. userId={}, couponId={}",
                                event.getUserId(), event.getCouponId());
                    }
                });
    }

    /**
     * 쿠폰 사용 이벤트를 Kafka로 전송
     */
    public void sendCouponUsedEvent(CouponUsedEvent event) {
        String key = event.getUserId().toString();

        kafkaTemplate.send(COUPON_USED_TOPIC, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("쿠폰 사용 이벤트 전송 실패. orderId={}", event.getOrderId(), ex);
                    } else {
                        log.info("쿠폰 사용 이벤트 전송 성공. orderId={}", event.getOrderId());
                    }
                });
    }
}
