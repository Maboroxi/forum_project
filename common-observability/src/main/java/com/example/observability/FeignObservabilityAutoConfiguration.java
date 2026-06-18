package com.example.observability;

import com.example.common.constants.GatewayHeaders;
import feign.RequestInterceptor;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(name = "feign.RequestInterceptor")
public class FeignObservabilityAutoConfiguration {

    @Bean
    RequestInterceptor requestIdFeignInterceptor() {
        return template -> {
            String requestId = MDC.get(RequestContext.REQUEST_ID);
            if (requestId != null && !requestId.isBlank()) {
                template.header(GatewayHeaders.REQUEST_ID, requestId);
            }
        };
    }
}
