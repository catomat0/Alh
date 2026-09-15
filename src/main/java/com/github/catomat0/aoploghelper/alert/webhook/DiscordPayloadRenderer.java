package com.github.catomat0.aoploghelper.alert.webhook;

import com.github.catomat0.aoploghelper.alert.AlertEvent;

import java.util.function.Function;

import static com.github.catomat0.aoploghelper.alert.webhook.HttpWebhookClient.escapeJson;
import static com.github.catomat0.aoploghelper.alert.webhook.HttpWebhookClient.renderMdcInline;
import static com.github.catomat0.aoploghelper.alert.webhook.HttpWebhookClient.truncate;

/**
 * Renders an {@link AlertEvent} as a Discord webhook payload using the {@code content} field.
 * Discord caps {@code content} at 2000 characters — we truncate before serializing.
 */
public final class DiscordPayloadRenderer implements Function<AlertEvent, String> {

    private static final int MAX_CONTENT = 1_900;

    @Override
    public String apply(AlertEvent event) {
        String icon = switch (event.level()) {
            case "ERROR" -> "🚨"; // 🚨
            case "WARN" -> "⚠️";  // ⚠️
            default -> "ℹ️";      // ℹ️
        };
        String mdc = renderMdcInline(event.mdc());
        StringBuilder content = new StringBuilder();
        content.append(icon).append(" **").append(event.level()).append("** ")
                .append("`").append(event.loggerName()).append("`\n");
        if (!mdc.isEmpty()) {
            content.append("_").append(mdc).append("_\n");
        }
        content.append(event.message());
        if (event.stackTrace() != null && !event.stackTrace().isBlank()) {
            content.append("\n```").append(event.stackTrace()).append("```");
        }
        return "{\"content\":\"" + escapeJson(truncate(content.toString(), MAX_CONTENT)) + "\"}";
    }
}
