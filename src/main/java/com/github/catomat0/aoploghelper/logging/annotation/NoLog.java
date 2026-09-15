package com.github.catomat0.aoploghelper.logging.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Opt a method or class out of AOP logging even if it matches the configured pointcut.
 * <p>
 * Typical use: high-frequency internal helpers, health checks, or methods handling
 * sensitive data that shouldn't be traced at all.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface NoLog {
}
