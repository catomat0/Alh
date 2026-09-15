package com.github.catomat0.aoploghelper.alert.webhook;

import com.github.catomat0.aoploghelper.alert.AlertEvent;
import com.github.catomat0.aoploghelper.alert.AlhAlertProperties;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.function.Function;

/**
 * Minimal JDK {@link HttpClient}-based webhook client. Serializes each event into
 * a provider-specific JSON payload and POSTs it. No external HTTP dependencies.
 */
public class HttpWebhookClient implements WebhookClient {

    private final HttpClient http;
    private final AlhAlertProperties properties;
    private final Function<AlertEvent, String> payloadRenderer;

    public HttpWebhookClient(AlhAlertProperties properties,
                             Function<AlertEvent, String> payloadRenderer) {
        this.properties = properties;
        this.payloadRenderer = payloadRenderer;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()))
                .build();
    }

    @Override
    public void send(AlertEvent event) throws Exception {
        String url = properties.getWebhookUrl();
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("alh.alert.webhook-url is not configured");
        }
        String body = payloadRenderer.apply(event);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(properties.getReadTimeoutMs()))
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            throw new IllegalStateException(
                    "webhook returned HTTP " + status + ": " + truncate(response.body(), 300));
        }
    }

    static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    static String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder out = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\' -> out.append("\\\\");
                case '"' -> out.append("\\\"");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }

    static String renderMdcInline(Map<String, String> mdc) {
        if (mdc == null || mdc.isEmpty()) return "";
        StringBuilder out = new StringBuilder();
        for (Map.Entry<String, String> e : mdc.entrySet()) {
            if (!out.isEmpty()) out.append(" ");
            out.append(e.getKey()).append("=").append(e.getValue());
        }
        return out.toString();
    }
}
