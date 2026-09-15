package com.github.catomat0.aoploghelper.logging.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Override the global slow-call threshold for a specific method/class.
 * <p>
 * When elapsed time exceeds {@link #thresholdMs()} the aspect logs at WARN.
 * Below the threshold the usual INFO log line is emitted.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface LogSlow {

    /** Threshold in milliseconds. */
    long thresholdMs() default 500L;
}
