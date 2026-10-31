
package com.artelier.api.security;


public final class SecurityRoutes {

    private SecurityRoutes() {
    }

    // Public regardless of HTTP method
    public static final String[] PUBLIC_ROUTES = {
            "/auth/**",
            "/payments/webhook",
            "/swagger",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/webjars/**",
            "/actuator/health",
            "/actuator/health/liveness",
            "/actuator/health/readiness"
    };

    // Public GET endpoints
    public static final String[] PUBLIC_GET_ROUTES = {
            "/categories/**",
            "/products/**",
            "/banners"
    };
}
