package com.github.catomat0.aoploghelper.alert.webhook;

import com.github.catomat0.aoploghelper.alert.AlertEvent;

/**
 * Strategy for shipping an {@link AlertEvent} to a webhook endpoint.
 * <p>
 * The default provider selection ({@code slack} vs {@code discord}) is driven by
 * {@code alh.alert.type}. Provide your own {@code @Bean WebhookClient} to override —
 * useful for Teams, PagerDuty, or an internal aggregator.
 */
public interface WebhookClient {

    void send(AlertEvent event) throws Exception;
}
