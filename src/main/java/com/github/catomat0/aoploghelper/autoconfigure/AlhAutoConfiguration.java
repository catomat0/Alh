package com.github.catomat0.aoploghelper.autoconfigure;

import com.github.catomat0.aoploghelper.alert.autoconfigure.AlhAlertAutoConfiguration;
import com.github.catomat0.aoploghelper.logging.autoconfigure.AlhLoggingAutoConfiguration;
import com.github.catomat0.aoploghelper.mdc.autoconfigure.AlhMdcAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Umbrella configuration that pulls in the individual pieces (MDC filter + AOP logging + alerts).
 * <p>
 * Each sub-config is independently gated by its own {@code alh.*.enabled} flag, so this
 * module simply groups them for consumers who prefer a single entry point.
 */
@AutoConfiguration
@Import({
        AlhMdcAutoConfiguration.class,
        AlhLoggingAutoConfiguration.class,
        AlhAlertAutoConfiguration.class
})
public class AlhAutoConfiguration {
}
