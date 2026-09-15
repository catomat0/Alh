package com.github.catomat0.aoploghelper.mdc;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Strategy for extracting the current user id to populate {@link AlhMdcKeys#USER_ID}.
 * <p>
 * Provide a {@code @Bean AlhUserIdResolver} to override the default behavior.
 * The default implementation returns {@code "anonymous"} for every request — apps that
 * use Spring Security typically want to plug in something like:
 *
 * <pre>{@code
 * @Bean
 * AlhUserIdResolver alhUserIdResolver() {
 *     return req -> {
 *         var auth = SecurityContextHolder.getContext().getAuthentication();
 *         if (auth != null && auth.getPrincipal() instanceof Long userId) {
 *             return String.valueOf(userId);
 *         }
 *         return "anonymous";
 *     };
 * }
 * }</pre>
 */
@FunctionalInterface
public interface AlhUserIdResolver {

    String resolve(HttpServletRequest request);
}
