package com.github.catomat0.aoploghelper.mdc;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Populates SLF4J {@link MDC} with per-request context so downstream logs can be correlated.
 * <p>
 * Keys written: {@link AlhMdcKeys#REQUEST_ID}, {@link AlhMdcKeys#USER_ID},
 * {@link AlhMdcKeys#METHOD}, {@link AlhMdcKeys#URI}.
 * <p>
 * If the incoming request already carries the configured header (default {@code X-Request-Id})
 * <b>and</b> the value passes the whitelist regex, that value is reused. Otherwise a fresh short
 * UUID prefix is generated. This prevents an attacker from smuggling arbitrary content (CRLF,
 * ANSI escape sequences, oversized payloads) into log lines via the correlation header.
 */
public class AlhMdcFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AlhMdcFilter.class);

    private static final Pattern REQUEST_ID_WHITELIST = Pattern.compile("[A-Za-z0-9._-]{1,64}");
    private static final int MAX_URI_LENGTH = 512;
    private static final int MAX_USER_ID_LENGTH = 128;

    private final AlhMdcProperties properties;
    private final AlhUserIdResolver userIdResolver;

    public AlhMdcFilter(AlhMdcProperties properties, AlhUserIdResolver userIdResolver) {
        this.properties = properties;
        this.userIdResolver = userIdResolver;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String requestId = resolveOrGenerateRequestId(request);
        try {
            MDC.put(AlhMdcKeys.REQUEST_ID, requestId);
            MDC.put(AlhMdcKeys.METHOD, sanitize(request.getMethod(), 16));
            MDC.put(AlhMdcKeys.URI, sanitize(request.getRequestURI(), MAX_URI_LENGTH));
            MDC.put(AlhMdcKeys.USER_ID, sanitize(safeResolveUserId(request), MAX_USER_ID_LENGTH));

            if (properties.isResponseHeader()) {
                response.setHeader(properties.getHeaderName(), requestId);
            }

            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(AlhMdcKeys.REQUEST_ID);
            MDC.remove(AlhMdcKeys.METHOD);
            MDC.remove(AlhMdcKeys.URI);
            MDC.remove(AlhMdcKeys.USER_ID);
        }
    }

    private String resolveOrGenerateRequestId(HttpServletRequest request) {
        String header = request.getHeader(properties.getHeaderName());
        if (header != null && !header.isBlank() && REQUEST_ID_WHITELIST.matcher(header).matches()) {
            return header;
        }
        int length = Math.min(32, Math.max(1, properties.getRequestIdLength()));
        return UUID.randomUUID().toString().replace("-", "").substring(0, length);
    }

    private String safeResolveUserId(HttpServletRequest request) {
        try {
            String resolved = userIdResolver.resolve(request);
            return (resolved == null || resolved.isBlank()) ? "anonymous" : resolved;
        } catch (RuntimeException e) {
            log.debug("AlhUserIdResolver threw {}; falling back to anonymous",
                    e.getClass().getSimpleName(), e);
            return "anonymous";
        }
    }

    private static String sanitize(String value, int maxLength) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        String trimmed = value.length() > maxLength ? value.substring(0, maxLength) : value;
        StringBuilder out = new StringBuilder(trimmed.length());
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c == '\n' || c == '\r' || c == '\t' || c == 0x1B || c < 0x20 || c == 0x7F) {
                out.append('?');
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}
