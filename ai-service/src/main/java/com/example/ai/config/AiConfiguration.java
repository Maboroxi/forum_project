package com.example.ai.config;

import com.example.common.constants.GatewayHeaders;
import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.core.task.support.ContextPropagatingTaskDecorator;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.concurrent.Executor;

@Configuration
public class AiConfiguration {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(60))
                .build();
    }

    @Bean("aiTaskExecutor")
    public Executor aiTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("ai-chat-");
        executor.setTaskDecorator(new ContextPropagatingTaskDecorator());
        executor.initialize();
        return executor;
    }

    @Bean
    public ThreadPoolTaskScheduler aiHeartbeatScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("ai-heartbeat-");
        scheduler.setTaskDecorator(new ContextPropagatingTaskDecorator());
        scheduler.initialize();
        return scheduler;
    }

    @Bean
    public RequestInterceptor internalServiceRequestInterceptor(
            @Value("${internal.service.token}") String token) {
        return template -> template.header(GatewayHeaders.INTERNAL_TOKEN, token);
    }
}
