package com.github.catomat0.aoploghelper.mdc;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the MDC filter.
 *
 * <pre>
 * alh:
 *   mdc:
 *     enabled: true          # false 면 필터 자체를 등록하지 않음
 *     request-id-length: 8   # UUID 앞 N 자리 사용 (1~32)
 *     header-name: X-Request-Id  # 상류에서 traceId 를 넘겨줄 헤더 이름 (없으면 새로 발급)
 *     response-header: true  # 응답 헤더에도 requestId 를 실어 내보낼지
 * </pre>
 */
@ConfigurationProperties(prefix = "alh.mdc")
public class AlhMdcProperties {

    private boolean enabled = true;
    private int requestIdLength = 8;
    private String headerName = "X-Request-Id";
    private boolean responseHeader = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getRequestIdLength() {
        return requestIdLength;
    }

    public void setRequestIdLength(int requestIdLength) {
        this.requestIdLength = requestIdLength;
    }

    public String getHeaderName() {
        return headerName;
    }

    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }

    public boolean isResponseHeader() {
        return responseHeader;
    }

    public void setResponseHeader(boolean responseHeader) {
        this.responseHeader = responseHeader;
    }
}
