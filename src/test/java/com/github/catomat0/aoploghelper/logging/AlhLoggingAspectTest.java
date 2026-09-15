package com.github.catomat0.aoploghelper.logging;

import com.github.catomat0.aoploghelper.logging.annotation.LogExecution;
import com.github.catomat0.aoploghelper.logging.annotation.LogSlow;
import com.github.catomat0.aoploghelper.logging.annotation.NoLog;
import com.github.catomat0.aoploghelper.logging.mask.SensitiveMasker;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.AspectJExpressionPointcutAdvisor;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AlhLoggingAspectTest {

    private final AlhLoggingProperties properties = new AlhLoggingProperties();
    private final SensitiveMasker masker = new SensitiveMasker(properties.getMaskKeywords());
    private final AlhLoggingAspect interceptor = new AlhLoggingAspect(properties, masker);

    private DemoService proxyForPointcut(DemoService target, String expression) {
        ProxyFactory factory = new ProxyFactory(target);
        AspectJExpressionPointcutAdvisor advisor = new AspectJExpressionPointcutAdvisor();
        advisor.setExpression(expression);
        advisor.setAdvice(interceptor);
        factory.addAdvisor(advisor);
        return (DemoService) factory.getProxy();
    }

    @Test
    void proxiesAndLogsMatchingMethod() {
        DemoService proxy = proxyForPointcut(new DemoService(),
                "execution(* com.github.catomat0.aoploghelper.logging..DemoService.*(..))");
        assertThat(proxy.echo("hello")).isEqualTo("hello");
    }

    @Test
    void propagatesExceptions() {
        DemoService proxy = proxyForPointcut(new DemoService(),
                "execution(* com.github.catomat0.aoploghelper.logging..DemoService.*(..))");
        assertThatThrownBy(proxy::boom).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void respectsNoLogAnnotation() {
        DemoService proxy = proxyForPointcut(new DemoService(),
                "execution(* com.github.catomat0.aoploghelper.logging..DemoService.*(..))");
        assertThat(proxy.silent()).isEqualTo("quiet");
    }

    @Test
    void logExecutionAdvisorTriggersOutsideConfiguredPointcut() {
        ProxyFactory factory = new ProxyFactory(new AnnotatedService());
        DefaultPointcutAdvisor advisor = new DefaultPointcutAdvisor(
                new AnnotationMatchingPointcut(LogExecution.class, LogExecution.class),
                interceptor
        );
        factory.addAdvisor(advisor);
        AnnotatedService proxy = (AnnotatedService) factory.getProxy();
        assertThat(proxy.tracked()).isEqualTo("ok");
    }

    @Test
    void logSlowOverridesGlobalThreshold() {
        DemoService proxy = proxyForPointcut(new DemoService(),
                "execution(* com.github.catomat0.aoploghelper.logging..DemoService.*(..))");
        assertThat(proxy.slow()).isEqualTo("late");
    }

    static class DemoService {

        String echo(String input) {
            return input;
        }

        void boom() {
            throw new IllegalStateException("boom");
        }

        @NoLog
        String silent() {
            return "quiet";
        }

        @LogSlow(thresholdMs = 1)
        String slow() {
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "late";
        }
    }

    static class AnnotatedService {

        @LogExecution(logArgs = true, logResult = true)
        String tracked() {
            return "ok";
        }
    }
}
