package com.github.catomat0.aoploghelper.alert.autoconfigure;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.github.catomat0.aoploghelper.alert.AlertEvent;
import com.github.catomat0.aoploghelper.alert.AlertRateLimiter;
import com.github.catomat0.aoploghelper.alert.AlhAlertProperties;
import com.github.catomat0.aoploghelper.alert.AlhWebhookAppender;
import com.github.catomat0.aoploghelper.alert.webhook.DiscordPayloadRenderer;
import com.github.catomat0.aoploghelper.alert.webhook.HttpWebhookClient;
import com.github.catomat0.aoploghelper.alert.webhook.SlackPayloadRenderer;
import com.github.catomat0.aoploghelper.alert.webhook.WebhookClient;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.function.Function;

@AutoConfiguration
@ConditionalOnClass(LoggerContext.class)
@ConditionalOnProperty(prefix = "alh.alert", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(AlhAlertProperties.class)
public class AlhAlertAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AlertRateLimiter alhAlertRateLimiter(AlhAlertProperties properties) {
        return new AlertRateLimiter(properties.getRateLimitPerMinute());
    }

    @Bean
    @ConditionalOnMissingBean
    public WebhookClient alhWebhookClient(AlhAlertProperties properties) {
        Function<AlertEvent, String> renderer = switch (properties.getType()) {
            case SLACK -> new SlackPayloadRenderer();
            case DISCORD -> new DiscordPayloadRenderer();
        };
        return new HttpWebhookClient(properties, renderer);
    }

    @Bean
    public AlhWebhookAppenderRegistrar alhWebhookAppenderRegistrar(
            AlhAlertProperties properties,
            WebhookClient client,
            AlertRateLimiter rateLimiter) {
        return new AlhWebhookAppenderRegistrar(properties, client, rateLimiter);
    }

    public static class AlhWebhookAppenderRegistrar {

        private static final org.slf4j.Logger log =
                LoggerFactory.getLogger(AlhWebhookAppenderRegistrar.class);

        private final AlhAlertProperties properties;
        private final WebhookClient client;
        private final AlertRateLimiter rateLimiter;
        private AlhWebhookAppender appender;

        public AlhWebhookAppenderRegistrar(AlhAlertProperties properties,
                                           WebhookClient client,
                                           AlertRateLimiter rateLimiter) {
            this.properties = properties;
            this.client = client;
            this.rateLimiter = rateLimiter;
        }

        @PostConstruct
        public void attach() {
            if (properties.getWebhookUrl() == null || properties.getWebhookUrl().isBlank()) {
                log.warn("alh.alert is enabled but webhook-url is empty — appender not attached");
                return;
            }
            org.slf4j.ILoggerFactory factory = LoggerFactory.getILoggerFactory();
            if (!(factory instanceof LoggerContext context)) {
                log.warn("SLF4J is not backed by Logback ({}) — alh.alert appender not attached",
                        factory.getClass().getName());
                return;
            }
            appender = new AlhWebhookAppender(properties, client, rateLimiter);
            appender.setName("alhWebhook");
            appender.setContext(context);
            appender.start();

            Logger root = context.getLogger(Logger.ROOT_LOGGER_NAME);
            root.addAppender(appender);
            log.info("alh.alert appender attached (type={}, threshold={}, rateLimit={}/min)",
                    properties.getType(), properties.getThreshold(),
                    properties.getRateLimitPerMinute());
        }

        @PreDestroy
        public void detach() {
            if (appender == null) return;
            org.slf4j.ILoggerFactory factory = LoggerFactory.getILoggerFactory();
            if (factory instanceof LoggerContext context) {
                Logger root = context.getLogger(Logger.ROOT_LOGGER_NAME);
                root.detachAppender(appender);
            }
            appender.stop();
        }
    }
}
