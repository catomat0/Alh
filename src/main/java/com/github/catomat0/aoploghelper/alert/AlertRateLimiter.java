package com.github.catomat0.aoploghelper.alert;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Trivial per-minute counter used to keep the webhook from spamming when the app is
 * emitting a burst of errors (e.g. during a downstream outage).
 * <p>
 * {@code perMinute <= 0} disables the limit.
 */
public class AlertRateLimiter {

    private final int limitPerMinute;
    private final AtomicInteger count = new AtomicInteger();
    private final AtomicLong windowStartMs = new AtomicLong(System.currentTimeMillis());

    public AlertRateLimiter(int limitPerMinute) {
        this.limitPerMinute = limitPerMinute;
    }

    public boolean tryAcquire() {
        if (limitPerMinute <= 0) return true;
        long now = System.currentTimeMillis();
        long start = windowStartMs.get();
        if (now - start >= 60_000L) {
            if (windowStartMs.compareAndSet(start, now)) {
                count.set(0);
            }
        }
        return count.incrementAndGet() <= limitPerMinute;
    }
}
