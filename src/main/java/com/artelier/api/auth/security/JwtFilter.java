package com.artelier.api.auth.security;

import com.artelier.api.auth.service.UserSessionService;
import com.artelier.api.auth.util.SecurityRoutes;
import com.artelier.api.user.entity.User;
import com.artelier.api.user.enums.UserStatus;
import com.artelier.api.user.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

import static com.artelier.api.shared.util.SecurityPathMatcher.matchesAny;

/**
 * Filter responsible for intercepting incoming HTTP requests to validate JSON Web Tokens (JWT),
 * verify active session states, check user account status, and populate the Spring Security context.
 *
 * <p>Users in {@code PENDING_VERIFICATION} keep a valid session, but are only allowed to reach
 * the routes listed in {@link SecurityRoutes#USER_PENDING_VERIFICATION_ROUTES}. Any other
 * protected route answers 403, while public GET routes behave as if the request were anonymous.</p>
 */
@AllArgsConstructor
@Component
public class JwtFilter extends OncePerRequestFilter {

    private final UserJwtService userJwtService;
    private final UserRepository userRepository;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final AccessDeniedHandler accessDeniedHandler;
    private final UserSessionService userSessionService;

    /**
     * Determines whether the filter should be bypassed for specific request paths.
     *
     * <p>Excludes public authentication endpoints (such as login and registration) from token filtering.</p>
     *
     * @param request the current HTTP request
     * @return {@code true} if the filter should be skipped; {@code false} otherwise
     */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return matchesAny(
                SecurityRoutes.PUBLIC_AUTH_ROUTES,
                request.getServletPath()
        );
    }

    /**
     * Checks whether the requested path is allowed for users whose accounts are still pending verification.
     *
     * @param request the current HTTP request
     * @return {@code true} if the path is accessible for pending verification users; {@code false} otherwise
     */
    protected boolean isAllowedForPendingUser(HttpServletRequest request) {
        return matchesAny(
                SecurityRoutes.USER_PENDING_VERIFICATION_ROUTES,
                request.getServletPath()
        );
    }

    /**
     * Checks whether the request targets a route that is public for GET requests (e.g., the catalog).
     *
     * @param request the current HTTP request
     * @return {@code true} if the method is GET and the path is in the public GET routes
     */
    private boolean isPublicGet(HttpServletRequest request) {
        return HttpMethod.GET.matches(request.getMethod())
                && matchesAny(SecurityRoutes.PUBLIC_GET_ROUTES, request.getServletPath());
    }

    /**
     * Clears the security context and answers 401 through the custom authentication entry point.
     *
     * @param request  the current HTTP request
     * @param response the HTTP response
     * @throws IOException if an I/O error occurs during forwarding or writing
     */
    private void rejectUnauthorized(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        SecurityContextHolder.clearContext();
        authenticationEntryPoint.commence(
                request,
                response,
                new BadCredentialsException("Invalid authentication credentials")
        );
    }

    /**
     * Applies the access rules that depend on the user's account status.
     *
     * <ul>
     *   <li>{@code ACTIVE}: full access.</li>
     *   <li>{@code PENDING_VERIFICATION}: only the allowed pending routes; public GET routes
     *       continue as anonymous; everything else answers 403.</li>
     *   <li>Any other status (e.g., {@code SUSPENDED}): 401.</li>
     * </ul>
     *
     * @param user        the authenticated user entity
     * @param request     the current HTTP request
     * @param response    the HTTP response
     * @param filterChain the filter chain for executing subsequent filters
     * @return {@code true} if the request may continue to be authenticated;
     *         {@code false} if the response was already handled here
     * @throws IOException      if an I/O error occurs
     * @throws ServletException if a servlet error occurs
     */
    private boolean handleUserStatus(
            User user,
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws IOException, ServletException {

        UserStatus status = user.getStatus();

        if (status == UserStatus.ACTIVE) {
            return true;
        }

        if (status != UserStatus.PENDING_VERIFICATION) {
            rejectUnauthorized(request, response);
            return false;
        }

        if (isAllowedForPendingUser(request)) {
            return true;
        }

        if (isPublicGet(request)) {
            filterChain.doFilter(request, response);
        } else {
            SecurityContextHolder.clearContext();
            accessDeniedHandler.handle(
                    request,
                    response,
                    new AccessDeniedException("Email verification required")
            );
        }
        return false;
    }

    /**
     * Extracts the JWT from the Authorization header, validates it, checks session and user status,
     * and sets the authentication in the security context.
     *
     * @param request     the current HTTP request
     * @param response    the HTTP response
     * @param filterChain the filter chain for executing subsequent filters
     * @throws ServletException if an error occurs during filtering
     * @throws IOException      if an I/O error occurs during filtering
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        if (!userJwtService.validateToken(token)) {
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        UUID userId;
        UUID sessionId;

        try {
            userId = userJwtService.extractUserId(token);
            sessionId = userJwtService.extractSessionId(token);
        } catch (JwtException | IllegalArgumentException e) {
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
            return;
        }

        if (!userSessionService.isActive(sessionId, userId)) {
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
            return;
        }

        User user = userRepository.findById(userId).orElse(null);

        if (user == null) {
            rejectUnauthorized(request, response);
            return;
        }

        if (!handleUserStatus(user, request, response, filterChain)) {
            return;
        }

        UserPrincipal principal = new UserPrincipal(user);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities()
                );
        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }
}