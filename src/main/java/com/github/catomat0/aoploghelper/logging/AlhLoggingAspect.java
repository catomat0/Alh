package com.github.catomat0.aoploghelper.logging;

import com.github.catomat0.aoploghelper.logging.annotation.LogExecution;
import com.github.catomat0.aoploghelper.logging.annotation.LogSlow;
import com.github.catomat0.aoploghelper.logging.annotation.NoLog;
import com.github.catomat0.aoploghelper.logging.mask.SensitiveMasker;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Method interceptor that emits a single log line per invocation with class, method,
 * (optionally masked) arguments, elapsed time, and — depending on outcome — the return
 * value or exception message.
 * <p>
 * Wired as two advisors in {@code AlhLoggingAutoConfiguration}:
 * <ul>
 *   <li>Configurable pointcut expression via {@code alh.logging.pointcut}.</li>
 *   <li>Annotation matcher for {@link LogExecution} (forced on).</li>
 * </ul>
 * Method-level annotation overrides:
 * <ul>
 *   <li>{@link NoLog} — bypasses logging even when matched.</li>
 *   <li>{@link LogSlow} — overrides {@code alh.logging.slow-threshold-ms}.</li>
 * </ul>
 */
public class AlhLoggingAspect implements MethodInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AlhLoggingAspect.class);

    private final AlhLoggingProperties properties;
    private final SensitiveMasker masker;

    public AlhLoggingAspect(AlhLoggingProperties properties, SensitiveMasker masker) {
        this.properties = properties;
        this.masker = masker;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Method method = invocation.getMethod();
        Object target = invocation.getThis();
        Class<?> targetClass = target != null ? target.getClass() : method.getDeclaringClass();

        if (isExcluded(method, targetClass)) {
            return invocation.proceed();
        }

        String className = targetClass.getSimpleName();
        String methodName = method.getName();
        long thresholdMs = resolveThresholdMs(method, targetClass);
        boolean logArgs = resolveLogArgs(method, targetClass);
        boolean logResult = resolveLogResult(method, targetClass);

        long start = System.currentTimeMillis();
        try {
            Object result = invocation.proceed();
            long elapsed = System.currentTimeMillis() - start;
            emitSuccess(className, methodName, elapsed, thresholdMs,
                    logArgs ? invocation.getArguments() : null,
                    result,
                    logResult);
            return result;
        } catch (Throwable e) {
            long elapsed = System.currentTimeMillis() - start;
            emitError(className, methodName, elapsed,
                    logArgs ? invocation.getArguments() : null, e);
            throw e;
        }
    }

    private boolean isExcluded(Method method, Class<?> targetClass) {
        return method.isAnnotationPresent(NoLog.class)
                || targetClass.isAnnotationPresent(NoLog.class);
    }

    private long resolveThresholdMs(Method method, Class<?> targetClass) {
        LogSlow onMethod = method.getAnnotation(LogSlow.class);
        if (onMethod != null) return onMethod.thresholdMs();
        LogSlow onClass = targetClass.getAnnotation(LogSlow.class);
        if (onClass != null) return onClass.thresholdMs();
        return properties.getSlowThresholdMs();
    }

    private boolean resolveLogArgs(Method method, Class<?> targetClass) {
        LogExecution onMethod = method.getAnnotation(LogExecution.class);
        if (onMethod != null) return onMethod.logArgs();
        LogExecution onClass = targetClass.getAnnotation(LogExecution.class);
        if (onClass != null) return onClass.logArgs();
        return properties.isLogArgs();
    }

    private boolean resolveLogResult(Method method, Class<?> targetClass) {
        LogExecution onMethod = method.getAnnotation(LogExecution.class);
        if (onMethod != null) return onMethod.logResult();
        LogExecution onClass = targetClass.getAnnotation(LogExecution.class);
        if (onClass != null) return onClass.logResult();
        return properties.isLogResult();
    }

    private void emitSuccess(String className, String methodName, long elapsed, long threshold,
                             Object[] args, Object result, boolean logResult) {
        boolean slow = elapsed >= threshold;
        String argsSegment = renderArgs(args);
        String resultSegment = logResult
                ? " result=" + masker.sanitizeForLog(String.valueOf(masker.mask(result)))
                : "";
        if (slow) {
            log.warn("[{}] {}{} - {}ms (>= {}ms){}",
                    className, methodName, argsSegment, elapsed, threshold, resultSegment);
        } else {
            log.info("[{}] {}{} - {}ms{}",
                    className, methodName, argsSegment, elapsed, resultSegment);
        }
    }

    private void emitError(String className, String methodName, long elapsed,
                           Object[] args, Throwable e) {
        String argsSegment = renderArgs(args);
        String safeMessage = masker.sanitizeForLog(String.valueOf(e.getMessage()));
        log.error("[{}] {}{} - {}ms - {}: {}",
                className, methodName, argsSegment, elapsed,
                e.getClass().getSimpleName(), safeMessage);
    }

    private String renderArgs(Object[] args) {
        if (args == null) {
            return "";
        }
        List<Object> masked = masker.maskArgs(args);
        // Some args (DTOs / records) escape the recursive masker because their toString()
        // renders the raw fields, so we run a second string-level pass here.
        return "(" + masker.sanitizeForLog(masked.toString()) + ")";
    }
}
