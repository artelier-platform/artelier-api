package com.artelier.api.integration.mail.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BrevoAuthInterceptor")
class BrevoAuthInterceptorTest {

    private static final String API_KEY = "xkeysib-test-123456";

    private RequestInterceptor interceptor;

    @BeforeEach
    void setUp() {
        BrevoAuthInterceptor config = new BrevoAuthInterceptor();
        ReflectionTestUtils.setField(config, "apiKey", API_KEY);
        interceptor = config.brevoRequestInterceptor();
    }

    @Test
    @DisplayName("builds a non-null interceptor bean")
    void beanIsCreated() {
        assertThat(interceptor).isNotNull();
    }

    @Test
    @DisplayName("sends the API key in the api-key header, not as a Bearer token")
    void apiKeyHeader() {
        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        assertThat(template.headers().get("api-key")).containsExactly(API_KEY);
        assertThat(template.headers()).doesNotContainKey("Authorization");
    }

    @Test
    @DisplayName("asks for and sends JSON")
    void jsonHeaders() {
        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        assertThat(template.headers().get("accept")).containsExactly("application/json");
        assertThat(template.headers().get("content-type")).containsExactly("application/json");
    }
}
