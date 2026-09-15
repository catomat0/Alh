package com.github.catomat0.aoploghelper.logging;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for the AOP logging aspect.
 *
 * <pre>
 * alh:
 *   logging:
 *     enabled: true
 *     pointcut: "execution(* com..*Service*.*(..)) || execution(* com..*Controller*.*(..))"
 *     log-args: true         # 파라미터 로깅
 *     log-result: false      # 반환값 로깅 (payload 큰 경우 부하 주의)
 *     slow-threshold-ms: 500 # 이 값 초과 시 WARN 으로 로깅
 *     mask-keywords:         # 민감정보 마스킹 대상 키워드 (부분 일치, 대소문자 무시)
 *       - password
 *       - passwd
 *       - secret
 *       - token
 *       - accessToken
 *       - refreshToken
 *       - authorization
 *       - apiKey
 *       - creditCard
 *       - ssn
 * </pre>
 */
@ConfigurationProperties(prefix = "alh.logging")
public class AlhLoggingProperties {

    private boolean enabled = true;

    private String pointcut =
            "execution(* *..*Service*.*(..)) || execution(* *..*Controller*.*(..))";

    private boolean logArgs = true;
    private boolean logResult = false;

    private long slowThresholdMs = 500L;

    private List<String> maskKeywords = new ArrayList<>(List.of(
            "password", "passwd", "pwd", "secret", "token",
            "accessToken", "refreshToken", "authorization", "apiKey",
            "creditCard", "ssn"
    ));

    /**
     * When true, applies regex-based masking for common PII (e-mail, KR mobile phone,
     * credit card, KR RRN) in addition to keyword-based masking.
     */
    private boolean maskPii = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getPointcut() {
        return pointcut;
    }

    public void setPointcut(String pointcut) {
        this.pointcut = pointcut;
    }

    public boolean isLogArgs() {
        return logArgs;
    }

    public void setLogArgs(boolean logArgs) {
        this.logArgs = logArgs;
    }

    public boolean isLogResult() {
        return logResult;
    }

    public void setLogResult(boolean logResult) {
        this.logResult = logResult;
    }

    public long getSlowThresholdMs() {
        return slowThresholdMs;
    }

    public void setSlowThresholdMs(long slowThresholdMs) {
        this.slowThresholdMs = slowThresholdMs;
    }

    public List<String> getMaskKeywords() {
        return maskKeywords;
    }

    public void setMaskKeywords(List<String> maskKeywords) {
        this.maskKeywords = maskKeywords;
    }

    public boolean isMaskPii() {
        return maskPii;
    }

    public void setMaskPii(boolean maskPii) {
        this.maskPii = maskPii;
    }
}
