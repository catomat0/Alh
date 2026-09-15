package com.github.catomat0.aoploghelper.mdc;

/**
 * MDC keys populated by {@link AlhMdcFilter}.
 * <p>
 * Reference these constants from your {@code logback-spring.xml} pattern via
 * {@code %X{requestId}}, {@code %X{userId}}, {@code %X{method}}, {@code %X{uri}}.
 */
public final class AlhMdcKeys {

    /** Short random correlation id issued per HTTP request (default 8 hex chars). */
    public static final String REQUEST_ID = "requestId";

    /** Resolved user id (or "anonymous" when no principal is present). */
    public static final String USER_ID = "userId";

    /** HTTP method (GET, POST, ...). */
    public static final String METHOD = "method";

    /** Request URI (path without query string). */
    public static final String URI = "uri";

    private AlhMdcKeys() {
    }
}
