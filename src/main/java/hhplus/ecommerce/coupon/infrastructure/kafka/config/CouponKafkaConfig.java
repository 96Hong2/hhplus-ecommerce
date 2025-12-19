package hhplus.ecommerce.coupon.infrastructure.kafka.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * 쿠폰 관련 Kafka 토픽 설정
 */
@Configuration
public class CouponKafkaConfig {

    public static final String COUPON_ISSUED_TOPIC = "coupon-issued";
    public static final String COUPON_USED_TOPIC = "coupon-used";

    /**
     * 쿠폰 발급 토픽
     * - 파티션 3개: Consumer 3개까지 병렬 처리 가능
     * - Replica 1개: 개발 환경 기준
     */
    @Bean
    public NewTopic couponIssuedTopic() {
        return TopicBuilder.name(COUPON_ISSUED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    /**
     * 쿠폰 사용 토픽
     */
    @Bean
    public NewTopic couponUsedTopic() {
        return TopicBuilder.name(COUPON_USED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
