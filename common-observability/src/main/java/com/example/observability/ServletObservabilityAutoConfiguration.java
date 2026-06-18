package com.example.observability;

import io.micrometer.tracing.Tracer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(name = "jakarta.servlet.Filter")
public class ServletObservabilityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ServletAccessLogFilter.class)
    ServletAccessLogFilter servletAccessLogFilter(Tracer tracer) {
        return new ServletAccessLogFilter(tracer);
    }
}
