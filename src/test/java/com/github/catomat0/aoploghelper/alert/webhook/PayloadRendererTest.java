package com.github.catomat0.aoploghelper.alert.webhook;

import com.github.catomat0.aoploghelper.alert.AlertEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PayloadRendererTest {

    private AlertEvent sample() {
        Map<String, String> mdc = new LinkedHashMap<>();
        mdc.put("requestId", "abc123");
        mdc.put("userId", "42");
        return new AlertEvent(
                Instant.parse("2026-09-15T10:00:00Z"),
                "ERROR",
                "com.example.MyService",
                "http-nio-8080-exec-1",
                "database timeout",
                mdc,
                "java.sql.SQLException: timeout\n  at com.example.MyService.foo(MyService.java:10)"
        );
    }

    @Test
    void slackPayloadContainsMdcAndMessage() {
        String body = new SlackPayloadRenderer().apply(sample());
        assertThat(body).startsWith("{\"text\":\"");
        assertThat(body).contains("ERROR");
        assertThat(body).contains("com.example.MyService");
        assertThat(body).contains("requestId=abc123");
        assertThat(body).contains("userId=42");
        assertThat(body).contains("database timeout");
    }

    @Test
    void discordPayloadContainsMdcAndMessage() {
        String body = new DiscordPayloadRenderer().apply(sample());
        assertThat(body).startsWith("{\"content\":\"");
        assertThat(body).contains("ERROR");
        assertThat(body).contains("requestId=abc123");
        assertThat(body).contains("database timeout");
    }

    @Test
    void slackPayloadEscapesQuotesAndNewlines() {
        AlertEvent event = new AlertEvent(
                Instant.now(), "ERROR", "logger", "t", "hello \"world\"\nsecond line",
                Map.of(), null
        );
        String body = new SlackPayloadRenderer().apply(event);
        assertThat(body).contains("\\\"world\\\"");
        assertThat(body).doesNotContain("\nsecond line"); // must be escaped
        assertThat(body).contains("\\n");
    }
}
