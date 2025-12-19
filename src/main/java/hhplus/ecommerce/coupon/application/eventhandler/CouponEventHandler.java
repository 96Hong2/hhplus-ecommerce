package hhplus.ecommerce.coupon.application.eventhandler;

import hhplus.ecommerce.coupon.domain.event.CouponIssuedEvent;
import hhplus.ecommerce.coupon.domain.event.CouponUsedEvent;
import hhplus.ecommerce.coupon.infrastructure.kafka.producer.CouponEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Spring Event를 받아서 Kafka로 발행하는 브릿지
 * 트랜잭션 커밋 후 실행되어 데이터 정합성 보장
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CouponEventHandler {

    private final CouponEventProducer couponEventProducer;

    /**
     * 쿠폰 발급 이벤트를 Kafka로 전송
     * - 트랜잭션 커밋 후 실행
     * - Kafka 전송 실패해도 핵심 비즈니스에 영향 없음
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCouponIssued(CouponIssuedEvent event) {
        try {
            couponEventProducer.sendCouponIssuedEvent(event);
        } catch (Exception e) {
            log.error("쿠폰 발급 이벤트 전송 실패. userCouponId={}", event.getUserCouponId(), e);
            // Kafka 전송 실패는 핵심 비즈니스에 영향 없음 (로그만 남김)
        }
    }

    /**
     * 쿠폰 사용 이벤트를 Kafka로 전송
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCouponUsed(CouponUsedEvent event) {
        try {
            couponEventProducer.sendCouponUsedEvent(event);
        } catch (Exception e) {
            log.error("쿠폰 사용 이벤트 전송 실패. userCouponId={}", event.getUserCouponId(), e);
        }
    }
}
