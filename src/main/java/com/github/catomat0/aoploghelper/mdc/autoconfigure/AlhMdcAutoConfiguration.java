package com.github.catomat0.aoploghelper.mdc.autoconfigure;

import com.github.catomat0.aoploghelper.mdc.AlhMdcFilter;
import com.github.catomat0.aoploghelper.mdc.AlhMdcProperties;
import com.github.catomat0.aoploghelper.mdc.AlhUserIdResolver;
import jakarta.servlet.Filter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

@AutoConfiguration
@ConditionalOnClass(Filter.class)
@ConditionalOnProperty(prefix = "alh.mdc", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(AlhMdcProperties.class)
public class AlhMdcAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AlhUserIdResolver alhUserIdResolver() {
        return request -> "anonymous";
    }

    @Bean
    @ConditionalOnMissingBean
    public AlhMdcFilter alhMdcFilter(AlhMdcProperties properties, AlhUserIdResolver resolver) {
        return new AlhMdcFilter(properties, resolver);
    }

    @Bean
    @ConditionalOnMissingBean(name = "alhMdcFilterRegistration")
    public FilterRegistrationBean<AlhMdcFilter> alhMdcFilterRegistration(AlhMdcFilter filter) {
        FilterRegistrationBean<AlhMdcFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        registration.addUrlPatterns("/*");
        registration.setName("alhMdcFilter");
        return registration;
    }
}
