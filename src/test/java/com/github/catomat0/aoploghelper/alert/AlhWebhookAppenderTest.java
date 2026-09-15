package com.github.catomat0.aoploghelper.alert;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import com.github.catomat0.aoploghelper.alert.webhook.WebhookClient;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class AlhWebhookAppenderTest {

    private final AlhAlertProperties properties = new AlhAlertProperties();

    @Test
    void dispatchesErrorEventToWebhookClient() {
        properties.setThreshold(AlhAlertProperties.Threshold.ERROR);
        List<AlertEvent> received = new ArrayList<>();
        WebhookClient client = received::add;
        AlhWebhookAppender appender = new AlhWebhookAppender(properties, client, new AlertRateLimiter(0));
        appender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        appender.start();

        appender.doAppend(newEvent(Level.ERROR, "boom"));

        await().atMost(2, TimeUnit.SECONDS).until(() -> !received.isEmpty());
        assertThat(received).hasSize(1);
        assertThat(received.get(0).level()).isEqualTo("ERROR");
        assertThat(received.get(0).message()).isEqualTo("boom");
        appender.stop();
    }

    @Test
    void skipsBelowThreshold() {
        properties.setThreshold(AlhAlertProperties.Threshold.ERROR);
        List<AlertEvent> received = new ArrayList<>();
        WebhookClient client = received::add;
        AlhWebhookAppender appender = new AlhWebhookAppender(properties, client, new AlertRateLimiter(0));
        appender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        appender.start();

        appender.doAppend(newEvent(Level.WARN, "just a warn"));
        appender.doAppend(newEvent(Level.INFO, "info"));

        // Give the executor a moment to prove nothing was dispatched.
        try {
            Thread.sleep(150);
        } catch (InterruptedException ignored) {
        }
        assertThat(received).isEmpty();
        appender.stop();
    }

    @Test
    void ignoresBlocklistedLogger() {
        properties.setThreshold(AlhAlertProperties.Threshold.ERROR);
        properties.setLoggerNamePrefixBlocklist(List.of("com.example.noisy"));
        List<AlertEvent> received = new ArrayList<>();
        WebhookClient client = received::add;
        AlhWebhookAppender appender = new AlhWebhookAppender(properties, client, new AlertRateLimiter(0));
        appender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        appender.start();

        LoggingEvent noisy = newEvent(Level.ERROR, "should skip");
        noisy.setLoggerName("com.example.noisy.Service");
        appender.doAppend(noisy);

        try {
            Thread.sleep(150);
        } catch (InterruptedException ignored) {
        }
        assertThat(received).isEmpty();
        appender.stop();
    }

    @Test
    void respectsRateLimit() {
        properties.setThreshold(AlhAlertProperties.Threshold.ERROR);
        List<AlertEvent> received = new ArrayList<>();
        WebhookClient client = received::add;
        AlhWebhookAppender appender = new AlhWebhookAppender(properties, client, new AlertRateLimiter(2));
        appender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        appender.start();

        for (int i = 0; i < 10; i++) {
            appender.doAppend(newEvent(Level.ERROR, "burst " + i));
        }

        await().atMost(2, TimeUnit.SECONDS).until(() -> received.size() >= 2);
        try {
            Thread.sleep(100);
        } catch (InterruptedException ignored) {
        }
        assertThat(received).hasSize(2);
        appender.stop();
    }

    @Test
    void webhookClientFailureDoesNotThrow() {
        properties.setThreshold(AlhAlertProperties.Threshold.ERROR);
        WebhookClient client = event -> {
            throw new RuntimeException("network down");
        };
        AlhWebhookAppender appender = new AlhWebhookAppender(properties, client, new AlertRateLimiter(0));
        appender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        appender.start();

        appender.doAppend(newEvent(Level.ERROR, "boom"));

        // The synchronous append must return; the failure is swallowed on the dispatcher thread.
        appender.stop();
    }

    private LoggingEvent newEvent(Level level, String message) {
        Logger logger = (Logger) LoggerFactory.getLogger("com.example.MyService");
        LoggingEvent event = new LoggingEvent(
                "com.example.MyService",
                logger,
                level,
                message,
                null,
                null
        );
        event.setTimeStamp(System.currentTimeMillis());
        return event;
    }
}
