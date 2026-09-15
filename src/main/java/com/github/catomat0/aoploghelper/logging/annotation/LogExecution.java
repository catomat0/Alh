package com.github.catomat0.aoploghelper.logging.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Force logging on a specific method/class regardless of the default pointcut.
 * <p>
 * Useful for methods that live outside the auto-scanned packages, or for turning
 * logging back on inside a class that has been {@link NoLog}-ed.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface LogExecution {

    /** Log arguments passed to the method. */
    boolean logArgs() default true;

    /** Log the return value. Off by default to avoid dumping large payloads. */
    boolean logResult() default false;
}
