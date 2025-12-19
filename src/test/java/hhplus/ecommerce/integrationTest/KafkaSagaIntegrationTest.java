package hhplus.ecommerce.integrationTest;

import hhplus.ecommerce.common.event.EventPublisher;
import hhplus.ecommerce.integration.application.service.ExternalIntegrationService;
import hhplus.ecommerce.order.application.dto.OrderItemInfo;
import hhplus.ecommerce.order.application.service.OrderService;
import hhplus.ecommerce.order.domain.event.OrderCreatedEvent;
import hhplus.ecommerce.order.domain.model.Order;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Kafka 기반 Saga 패턴 통합 테스트 (간소화 버전)
 */
@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(
    partitions = 1,
    topics = {"order-created", "order-integration-failed", "order-compensated"}
)
class KafkaSagaIntegrationTest {

    @Autowired
    private EventPublisher eventPublisher;

    @MockBean
    private ExternalIntegrationService externalIntegrationService;

    @MockBean
    private OrderService orderService;

    @Test
    @DisplayName("주문 생성 → 외부 연동 성공 시나리오")
    void testOrderCreatedAndIntegrationSuccess() {
        // Given
        when(orderService.getOrder(anyLong())).thenReturn(mock(Order.class));
        doNothing().when(externalIntegrationService).sendOrderToERP(any());

        OrderCreatedEvent event = createOrderCreatedEvent(1L, "ORDER-001");

        // When
        eventPublisher.publish(event);

        // Then
        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(externalIntegrationService, atLeastOnce()).sendOrderToERP(any()));
    }

    private OrderCreatedEvent createOrderCreatedEvent(Long orderId, String orderNumber) {
        List<OrderItemInfo> orderItems = List.of(
                new OrderItemInfo(1L, 1L, "상품1", 2, BigDecimal.valueOf(10000), BigDecimal.valueOf(20000))
        );

        return new OrderCreatedEvent(
                orderId, orderNumber, 100L,
                BigDecimal.valueOf(20000), BigDecimal.ZERO, BigDecimal.valueOf(20000),
                null, orderItems
        );
    }
}
