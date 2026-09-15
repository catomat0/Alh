package com.github.catomat0.aoploghelper.alert.webhook;

import com.github.catomat0.aoploghelper.alert.AlertEvent;

import java.util.function.Function;

import static com.github.catomat0.aoploghelper.alert.webhook.HttpWebhookClient.escapeJson;
import static com.github.catomat0.aoploghelper.alert.webhook.HttpWebhookClient.renderMdcInline;
import static com.github.catomat0.aoploghelper.alert.webhook.HttpWebhookClient.truncate;

/**
 * Renders an {@link AlertEvent} as a Slack webhook payload using the {@code text} field.
 * The Slack incoming-webhook contract accepts up to ~40KB of body; we trim aggressively
 * so a runaway stack trace never eats the caller's daily quota.
 */
public final class SlackPayloadRenderer implements Function<AlertEvent, String> {

    private static final int MAX_TEXT = 30_000;

    @Override
    public String apply(AlertEvent event) {
        String icon = switch (event.level()) {
            case "ERROR" -> ":rotating_light:";
            case "WARN" -> ":warning:";
            default -> ":information_source:";
        };
        String mdc = renderMdcInline(event.mdc());
        StringBuilder text = new StringBuilder();
        text.append(icon).append(" *").append(event.level()).append("* ")
                .append("`").append(event.loggerName()).append("`\n");
        if (!mdc.isEmpty()) {
            text.append("_").append(mdc).append("_\n");
        }
        text.append(event.message());
        if (event.stackTrace() != null && !event.stackTrace().isBlank()) {
            text.append("\n```").append(event.stackTrace()).append("```");
        }
        return "{\"text\":\"" + escapeJson(truncate(text.toString(), MAX_TEXT)) + "\"}";
    }
}
