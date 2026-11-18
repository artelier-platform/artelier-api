package com.artelier.api.auth.util;

import java.util.List;

/**
 * Utility class containing route patterns used by the security filter chain
 * and WebSecurity configuration to permit public access or restricted role levels without authentication.
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
    public static final List<String> PUBLIC_ROUTES = List.of(
            "/auth/**",
            "/payments/webhook",
            "/user/emails/verify",
            "/swagger",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/webjars/**",
            "/actuator/health",
            "/actuator/health/liveness",
            "/actuator/health/readiness"
    );

    /**
     * Endpoints accessible to users whose accounts are still pending email verification
     * (e.g., managing email verification or maintaining session lifecycle via refresh/logout).
     */
    public static final List<String> USER_PENDING_VERIFICATION_ROUTES = List.of(
            "/user/emails/**",
            "/auth/refresh",
            "/auth/logout"
    );

    /**
     * Authentication endpoints that are completely public, allowing unauthenticated users
     * to register new accounts or log in to obtain access tokens.
     */
    public static final List<String> PUBLIC_AUTH_ROUTES = List.of(
            "/auth/register",
            "/auth/login"
    );

    /**
     * Endpoints that allow public access exclusively for HTTP GET requests.
     * Typically includes catalog browsing routes such as categories, products, and banners.
     */
    public static final List<String> PUBLIC_GET_ROUTES = List.of(
            "/categories/**",
            "/products/**",
            "/banners"
    );
}