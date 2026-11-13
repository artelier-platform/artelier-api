package com.artelier.api.security;

import com.artelier.api.entity.User;
import com.artelier.api.enums.UserStatus;
import com.artelier.api.repository.UserRepository;
import com.artelier.api.service.UserSessionService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter responsible for intercepting incoming HTTP requests to validate JSON Web Tokens (JWT), 
 * check user account status, verify active session states, and populate the Spring Security context.
 */
@AllArgsConstructor
@Component
public class JwtFilter extends OncePerRequestFilter {

    private final UserJwtService userJwtService;
    private final UserRepository userRepository;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final UserSessionService userSessionService;

    /**
     * Determines whether the filter should be bypassed for specific request paths.
     *
     * <p>Excludes authentication endpoints (starting with {@code /auth/}) from token filtering.</p>
     *
     * @param request the current HTTP request
     * @return {@code true} if the filter should be skipped; {@code false} otherwise
     */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI()
                .substring(request.getContextPath().length());

        return path.startsWith("/auth/");
    }

    /**
     * Extracts the JWT from the Authorization header, validates its cryptographic signature and claims,
     * checks user and session validity, and sets the authentication in the security context.
     *
     * @param request     the current HTTP request
     * @param response    the HTTP response
     * @param filterChain the filter chain for executing subsequent filters
     * @throws ServletException if an error occurs during filtering
     * @throws IOException      if an I/O error occurs during filtering
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
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

        User user = userRepository.findById(userId).orElse(null);

        if (user == null ||
                (user.getStatus() != UserStatus.ACTIVE &&
                        user.getStatus() != UserStatus.PENDING_VERIFICATION)) {
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(
                    request,
                    response,
                    new BadCredentialsException(
                            "Invalid authentication credentials"
                    )
            );
            return;
        }

        if (!userSessionService.isActive(sessionId, userId)) {
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
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