
package com.artelier.api.integration.wompi.config;

import feign.RequestTemplate;
import feign.codec.ErrorDecoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class WompiAuthRequestInterceptorTest {

    private WompiAuthRequestInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new WompiAuthRequestInterceptor();
        ReflectionTestUtils.setField(
                interceptor,
                "privateKey",
                "prv_test_123456"
        );
    }

    @Test
    void apply_ShouldAddAuthorizationHeaderWithBearerToken() {
        // Arrange
        RequestTemplate template = new RequestTemplate();

        // Act
        interceptor.apply(template);

        // Assert
        assertThat(template.headers())
                .containsKey("Authorization");

        assertThat(template.headers().get("Authorization"))
                .containsExactly("Bearer prv_test_123456");
    }

    @Test
    void wompiErrorDecoder_ShouldReturnWompiErrorDecoderInstance() {
        // Act
        ErrorDecoder decoder = interceptor.wompiErrorDecoder();

        // Assert
        assertThat(decoder)
                .isNotNull()
                .isInstanceOf(WompiErrorDecoder.class);
    }
}
