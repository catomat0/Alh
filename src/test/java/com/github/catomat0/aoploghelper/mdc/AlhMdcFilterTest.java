package com.github.catomat0.aoploghelper.mdc;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class AlhMdcFilterTest {

    private AlhMdcProperties properties;
    private AlhMdcFilter filter;

    @BeforeEach
    void setUp() {
        properties = new AlhMdcProperties();
        filter = new AlhMdcFilter(properties, request -> "user-42");
    }

    @Test
    void populatesMdcAndClearsAfterwards() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/hello");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> {
            assertThat(MDC.get(AlhMdcKeys.REQUEST_ID)).isNotBlank();
            assertThat(MDC.get(AlhMdcKeys.METHOD)).isEqualTo("POST");
            assertThat(MDC.get(AlhMdcKeys.URI)).isEqualTo("/api/hello");
            assertThat(MDC.get(AlhMdcKeys.USER_ID)).isEqualTo("user-42");
        };

        filter.doFilter(request, response, chain);

        assertThat(MDC.get(AlhMdcKeys.REQUEST_ID)).isNull();
        assertThat(MDC.get(AlhMdcKeys.METHOD)).isNull();
        assertThat(response.getHeader(properties.getHeaderName())).isNotBlank();
    }

    @Test
    void reusesIncomingRequestIdHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        request.addHeader(properties.getHeaderName(), "abc123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        String[] captured = new String[1];
        FilterChain chain = (req, res) -> captured[0] = MDC.get(AlhMdcKeys.REQUEST_ID);

        filter.doFilter(request, response, chain);

        assertThat(captured[0]).isEqualTo("abc123");
        assertThat(response.getHeader(properties.getHeaderName())).isEqualTo("abc123");
    }

    @Test
    void rejectsMaliciousRequestIdHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        request.addHeader(properties.getHeaderName(), "abc\r\nX-Injected: 1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        String[] captured = new String[1];
        FilterChain chain = (req, res) -> captured[0] = MDC.get(AlhMdcKeys.REQUEST_ID);

        filter.doFilter(request, response, chain);

        assertThat(captured[0]).doesNotContain("\r").doesNotContain("\n");
        assertThat(captured[0]).isNotEqualTo("abc\r\nX-Injected: 1");
    }

    @Test
    void stripsControlCharsFromUri() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/\r\nfake");
        MockHttpServletResponse response = new MockHttpServletResponse();

        String[] captured = new String[1];
        FilterChain chain = (req, res) -> captured[0] = MDC.get(AlhMdcKeys.URI);

        filter.doFilter(request, response, chain);

        assertThat(captured[0]).doesNotContain("\r").doesNotContain("\n");
    }

    @Test
    void fallsBackToAnonymousWhenResolverThrows() throws Exception {
        AlhMdcFilter fallback = new AlhMdcFilter(properties, req -> {
            throw new IllegalStateException("no security context");
        });

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        MockHttpServletResponse response = new MockHttpServletResponse();

        String[] captured = new String[1];
        FilterChain chain = (req, res) -> captured[0] = MDC.get(AlhMdcKeys.USER_ID);

        fallback.doFilter(request, response, chain);

        assertThat(captured[0]).isEqualTo("anonymous");
    }
}
