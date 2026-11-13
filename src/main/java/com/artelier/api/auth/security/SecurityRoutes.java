package com.artelier.api.auth.security;

/**
 * Utility class containing route patterns used by the security filter chain 
 * and WebSecurity configuration to permit public access without authentication.
 */
public final class SecurityRoutes {

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private SecurityRoutes() {
    }

    /**
     * Endpoints that are completely public regardless of the HTTP method used (GET, POST, PUT, DELETE, etc.).
     * Includes authentication routes, payment webhooks, API documentation (Swagger/OpenAPI), 
     * and system health monitoring actuators.
     */
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

    /**
     * Endpoints that allow public access exclusively for HTTP GET requests.
     * Typically includes catalog browsing routes such as categories, products, and banners.
     */
    public static final String[] PUBLIC_GET_ROUTES = {
            "/categories/**",
            "/products/**",
            "/banners"
    };
}