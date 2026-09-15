package com.github.catomat0.aoploghelper.alert;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Configuration for the log-based webhook alert appender.
 *
 * <pre>
 * alh:
 *   alert:
 *     enabled: true
 *     type: slack                                    # slack | discord
 *     webhook-url: ${SLACK_WEBHOOK_URL}    # https://hooks.slack.com/services/...
 *     threshold: ERROR                               # ERROR | WARN
 *     include-mdc-keys: [requestId, userId, method, uri]
 *     include-stack-trace: true
 *     max-stack-lines: 20
 *     rate-limit-per-minute: 30                      # 0 = 무제한
 *     connect-timeout-ms: 3000
 *     read-timeout-ms: 5000
 *     logger-name-prefix-blocklist:                  # 여기서 시작하는 logger 이벤트는 무시
 *       - com.github.catomat0.aoploghelper.alert     # 웹훅 실패 로그가 다시 웹훅 발송 방지
 * </pre>
 */
@ConfigurationProperties(prefix = "alh.alert")
public class AlhAlertProperties {

    public enum Type { SLACK, DISCORD }
    public enum Threshold { ERROR, WARN }

    private boolean enabled = false;
    private Type type = Type.SLACK;
    private String webhookUrl;
    private Threshold threshold = Threshold.ERROR;
    private List<String> includeMdcKeys = List.of("requestId", "userId", "method", "uri");
    private boolean includeStackTrace = true;
    private int maxStackLines = 20;
    private int rateLimitPerMinute = 30;
    private int connectTimeoutMs = 3_000;
    private int readTimeoutMs = 5_000;
    private List<String> loggerNamePrefixBlocklist =
            List.of("com.github.catomat0.aoploghelper.alert");

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }

    public String getWebhookUrl() { return webhookUrl; }
    public void setWebhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; }

    public Threshold getThreshold() { return threshold; }
    public void setThreshold(Threshold threshold) { this.threshold = threshold; }

    public List<String> getIncludeMdcKeys() { return includeMdcKeys; }
    public void setIncludeMdcKeys(List<String> includeMdcKeys) { this.includeMdcKeys = includeMdcKeys; }

    public boolean isIncludeStackTrace() { return includeStackTrace; }
    public void setIncludeStackTrace(boolean includeStackTrace) { this.includeStackTrace = includeStackTrace; }

    public int getMaxStackLines() { return maxStackLines; }
    public void setMaxStackLines(int maxStackLines) { this.maxStackLines = maxStackLines; }

    public int getRateLimitPerMinute() { return rateLimitPerMinute; }
    public void setRateLimitPerMinute(int rateLimitPerMinute) { this.rateLimitPerMinute = rateLimitPerMinute; }

    public int getConnectTimeoutMs() { return connectTimeoutMs; }
    public void setConnectTimeoutMs(int connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }

    public int getReadTimeoutMs() { return readTimeoutMs; }
    public void setReadTimeoutMs(int readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }

    public List<String> getLoggerNamePrefixBlocklist() { return loggerNamePrefixBlocklist; }
    public void setLoggerNamePrefixBlocklist(List<String> loggerNamePrefixBlocklist) {
        this.loggerNamePrefixBlocklist = loggerNamePrefixBlocklist;
    }
}
