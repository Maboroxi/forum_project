package com.example.ai.filter;

import com.example.common.constants.GatewayHeaders;
import com.example.common.constants.RequestAttributes;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayIdentityFilterTest {

    private final GatewayIdentityFilter filter = new GatewayIdentityFilter("internal-secret");

    @Test
    void rejectsDirectRequestsWithoutInternalToken() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/ai/conversations");
        request.addHeader(GatewayHeaders.USER_ID, "1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Gateway");
    }

    @Test
    void acceptsGatewayIdentityAndSetsRequestAttribute() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/ai/conversations");
        request.addHeader(GatewayHeaders.INTERNAL_TOKEN, "internal-secret");
        request.addHeader(GatewayHeaders.USER_ID, "7");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(request.getAttribute(RequestAttributes.USER_ID)).isEqualTo(7);
        assertThat(chain.getRequest()).isSameAs(request);
    }
}
