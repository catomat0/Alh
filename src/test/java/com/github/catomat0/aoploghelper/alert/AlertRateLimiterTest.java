package com.github.catomat0.aoploghelper.alert;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AlertRateLimiterTest {

    @Test
    void allowsUpToLimitThenBlocks() {
        AlertRateLimiter limiter = new AlertRateLimiter(3);
        assertThat(limiter.tryAcquire()).isTrue();
        assertThat(limiter.tryAcquire()).isTrue();
        assertThat(limiter.tryAcquire()).isTrue();
        assertThat(limiter.tryAcquire()).isFalse();
        assertThat(limiter.tryAcquire()).isFalse();
    }

    @Test
    void zeroDisablesLimiting() {
        AlertRateLimiter limiter = new AlertRateLimiter(0);
        for (int i = 0; i < 1_000; i++) {
            assertThat(limiter.tryAcquire()).isTrue();
        }
    }
}
