package com.github.catomat0.aoploghelper.logging.autoconfigure;

import com.github.catomat0.aoploghelper.logging.AlhLoggingAspect;
import com.github.catomat0.aoploghelper.logging.AlhLoggingProperties;
import com.github.catomat0.aoploghelper.logging.annotation.LogExecution;
import com.github.catomat0.aoploghelper.logging.mask.SensitiveMasker;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.aspectj.AspectJExpressionPointcutAdvisor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.Ordered;

@AutoConfiguration
@ConditionalOnClass(Aspect.class)
@ConditionalOnProperty(prefix = "alh.logging", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(AlhLoggingProperties.class)
@EnableAspectJAutoProxy
public class AlhLoggingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SensitiveMasker alhSensitiveMasker(AlhLoggingProperties properties) {
        return new SensitiveMasker(properties.getMaskKeywords(), properties.isMaskPii());
    }

    @Bean
    @ConditionalOnMissingBean
    public AlhLoggingAspect alhLoggingAspect(AlhLoggingProperties properties, SensitiveMasker masker) {
        return new AlhLoggingAspect(properties, masker);
    }

    /**
     * Registers the configurable pointcut ({@code alh.logging.pointcut}) so any bean whose
     * method matches the expression will be logged. Falls back to a permissive default that
     * captures service and controller beans by naming convention.
     */
    @Bean
    public AspectJExpressionPointcutAdvisor alhLoggingPointcutAdvisor(
            AlhLoggingProperties properties, AlhLoggingAspect interceptor) {
        AspectJExpressionPointcutAdvisor advisor = new AspectJExpressionPointcutAdvisor();
        advisor.setExpression(properties.getPointcut());
        advisor.setAdvice(interceptor);
        advisor.setOrder(Ordered.LOWEST_PRECEDENCE - 100);
        return advisor;
    }

    /**
     * Registers a second advisor triggered by the {@link LogExecution} annotation so callers
     * can opt-in per-class/per-method regardless of the global pointcut.
     */
    @Bean
    public DefaultPointcutAdvisor alhLoggingAnnotationAdvisor(AlhLoggingAspect interceptor) {
        AnnotationMatchingPointcut pointcut = new AnnotationMatchingPointcut(
                LogExecution.class, LogExecution.class);
        DefaultPointcutAdvisor advisor = new DefaultPointcutAdvisor(pointcut, interceptor);
        advisor.setOrder(Ordered.LOWEST_PRECEDENCE - 200);
        return advisor;
    }
}
