package hhplus.ecommerce.common.application.util;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * AtomicLong 기반 자동 증가 ID 생성 유틸리티
 * 스레드 안전한 ID 생성 보장
 */
@Component
public class IdGenerator {

    private final AtomicLong sequence = new AtomicLong(1);

    /**
     * 다음 ID 생성
     * @return 자동 증가된 고유 ID
     */
    public Long generateId() {
        return sequence.getAndIncrement();
    }

    /**
     * 현재 ID 값 조회 (테스트용)
     * @return 현재 ID 값
     */
    public Long getCurrentId() {
        return sequence.get();
    }
}