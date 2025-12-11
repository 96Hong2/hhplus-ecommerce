package hhplus.ecommerce.common.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 도메인 이벤트 발행자
 *
 * Spring의 ApplicationEventPublisher를 래핑하여 도메인 레이어에서
 * Spring 프레임워크 의존성을 제거하고 이벤트 발행 추상화 제공
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * 도메인 이벤트 발행
     *
     * @param event 발행할 이벤트 객체
     */
    public void publish(Object event) {
        log.debug("-- 이벤트 발행: {}", event.getClass().getSimpleName());
        applicationEventPublisher.publishEvent(event);
    }
}
