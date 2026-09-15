package com.github.catomat0.aoploghelper.alert;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.AppenderBase;
import com.github.catomat0.aoploghelper.alert.webhook.WebhookClient;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Logback appender that forwards qualifying log events to a webhook.
 * <p>
 * Filtering happens on the logging thread (level threshold + logger-name blocklist +
 * rate limit), while the actual HTTP call runs on a bounded single-thread executor so
 * the application never blocks on outbound network I/O.
 */
public class AlhWebhookAppender extends AppenderBase<ILoggingEvent> {

    private final AlhAlertProperties properties;
    private final WebhookClient client;
    private final AlertRateLimiter rateLimiter;
    private final ThreadPoolExecutor executor;

    public AlhWebhookAppender(AlhAlertProperties properties,
                              WebhookClient client,
                              AlertRateLimiter rateLimiter) {
        this.properties = properties;
        this.client = client;
        this.rateLimiter = rateLimiter;
        this.executor = new ThreadPoolExecutor(
                1, 1, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(256),
                daemonFactory("alh-alert-dispatcher"),
                new ThreadPoolExecutor.DiscardOldestPolicy()
        );
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (!passesThreshold(event.getLevel())) return;
        if (isBlockedLogger(event.getLoggerName())) return;
        if (!rateLimiter.tryAcquire()) return;

        AlertEvent snapshot = snapshot(event);
        executor.execute(() -> {
            try {
                client.send(snapshot);
            } catch (Throwable e) {
                // Never surface via SLF4J or we recurse into this appender.
                System.err.println("[alh-alert] webhook send failed: "
                        + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        });
    }

    @Override
    public void stop() {
        super.stop();
        executor.shutdown();
        try {
            if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }

    private boolean passesThreshold(Level level) {
        int min = switch (properties.getThreshold()) {
            case ERROR -> Level.ERROR_INT;
            case WARN -> Level.WARN_INT;
        };
        return level.toInt() >= min;
    }

    private boolean isBlockedLogger(String loggerName) {
        if (loggerName == null) return false;
        List<String> blocklist = properties.getLoggerNamePrefixBlocklist();
        if (blocklist == null) return false;
        for (String prefix : blocklist) {
            if (loggerName.startsWith(prefix)) return true;
        }
        return false;
    }

    private AlertEvent snapshot(ILoggingEvent event) {
        Map<String, String> mdc = extractMdc(event.getMDCPropertyMap());
        String stack = properties.isIncludeStackTrace()
                ? renderStack(event.getThrowableProxy(), properties.getMaxStackLines())
                : null;
        return new AlertEvent(
                Instant.ofEpochMilli(event.getTimeStamp()),
                event.getLevel().toString(),
                event.getLoggerName(),
                event.getThreadName(),
                event.getFormattedMessage(),
                mdc,
                stack
        );
    }

    private Map<String, String> extractMdc(Map<String, String> full) {
        if (full == null || full.isEmpty()) return Map.of();
        List<String> keys = properties.getIncludeMdcKeys();
        if (keys == null || keys.isEmpty()) return Map.of();
        Map<String, String> filtered = new LinkedHashMap<>();
        for (String k : keys) {
            String v = full.get(k);
            if (v != null) filtered.put(k, v);
        }
        return filtered;
    }

    static String renderStack(IThrowableProxy proxy, int maxLines) {
        if (proxy == null) return null;
        StringBuilder sb = new StringBuilder();
        sb.append(proxy.getClassName()).append(": ").append(proxy.getMessage()).append("\n");
        StackTraceElementProxy[] elements = proxy.getStackTraceElementProxyArray();
        int limit = Math.min(elements.length, Math.max(1, maxLines));
        for (int i = 0; i < limit; i++) {
            sb.append("  at ").append(elements[i].getSTEAsString()).append("\n");
        }
        if (elements.length > limit) {
            sb.append("  ... ").append(elements.length - limit).append(" more");
        }
        return sb.toString();
    }

    private static ThreadFactory daemonFactory(String namePrefix) {
        AtomicInteger idx = new AtomicInteger();
        return runnable -> {
            Thread t = new Thread(runnable, namePrefix + "-" + idx.incrementAndGet());
            t.setDaemon(true);
            return t;
        };
    }

    /** For tests: expose the executor so we can wait for pending dispatches. */
    ThreadPoolExecutor executorForTest() {
        return executor;
    }

    /** For tests: force a fresh MDC map since Logback caches per event. */
    static Map<String, String> copyMdc(Map<String, String> src) {
        return src == null ? Map.of() : new HashMap<>(src);
    }
}
