package com.github.catomat0.aoploghelper.alert;

import java.time.Instant;
import java.util.Map;

/**
 * Immutable snapshot of a log event captured for asynchronous webhook delivery.
 * <p>
 * The appender takes this snapshot on the logging thread and hands it to a dispatcher
 * so we never block the calling code waiting for the HTTP call.
 */
public record AlertEvent(
        Instant timestamp,
        String level,
        String loggerName,
        String threadName,
        String message,
        Map<String, String> mdc,
        String stackTrace
) {
}
