package com.artelier.api.security;

import com.artelier.api.entity.User;
import com.artelier.api.enums.Role;
import com.artelier.api.enums.UserStatus;
import com.artelier.api.repository.UserRepository;
import com.artelier.api.service.UserSessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtFilter")
class JwtFilterTest {

    @Mock private JwtUtil jwtUtil;
    @Mock private UserRepository userRepository;
    @Mock private UserSessionService userSessionService;
    @Mock private CustomAuthenticationEntryPoint authenticationEntryPoint;

    @Mock private FilterChain filterChain;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;

    @InjectMocks private JwtFilter jwtFilter;

    private UUID userId;
    private UUID sessionId;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        userId = UUID.randomUUID();
        sessionId = UUID.randomUUID();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // =========================================================
    // Fixtures
    // =========================================================

    private User user(UserStatus status, Role role) {
        return User.builder()
                .id(userId).username("Geronimo01").passwordHash("hash")
                .role(role).status(status)
                .build();
    }

    private void givenBearer(String token) {
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
    }

    /** A correctly signed, unexpired token that carries a user id and a session id. */
    private void givenValidToken() {
        givenBearer("valid");
        when(jwtUtil.validateToken("valid")).thenReturn(true);
        // Lenient: depending on the order of checks, the filter may stop before reading the sid.
        lenient().when(jwtUtil.extractUserId("valid")).thenReturn(userId);
        lenient().when(jwtUtil.extractSessionId("valid")).thenReturn(sessionId);
    }

    /** Lenient: the filter may check the session before or after loading the user. */
    private void givenUserAndSession(User user, boolean sessionActive) {
        lenient().when(userRepository.findById(userId)).thenReturn(Optional.ofNullable(user));
        lenient().when(userSessionService.isActive(sessionId, userId)).thenReturn(sessionActive);
    }

    private void assertRejectedWith401() throws Exception {
        verify(authenticationEntryPoint).commence(
                eq(request), eq(response), any(BadCredentialsException.class));
        verify(filterChain, never()).doFilter(any(), any());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private void assertContinuesUnauthenticated() throws Exception {
        verify(filterChain).doFilter(request, response);
        verify(authenticationEntryPoint, never()).commence(any(), any(), any());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    // =========================================================
    // shouldNotFilter
    // =========================================================

    @ParameterizedTest(name = "{0} (context ''{1}'') → skipped: {2}")
    @CsvSource({
            "/auth/login,        '',     true",
            "/auth/refresh,      '',     true",
            "/api/auth/login,    /api,   true",
            "/users/abc/profile, '',     false",
            "/authors,           '',     false",
            "/products,          '',     false"
    })
    @DisplayName("public /auth/ endpoints are never filtered, so a stale Bearer cannot break them")
    void shouldNotFilter(String uri, String contextPath, boolean skipped) {
        when(request.getRequestURI()).thenReturn(uri);
        when(request.getContextPath()).thenReturn(contextPath);

        assertThat(jwtFilter.shouldNotFilter(request)).isEqualTo(skipped);
    }

    // =========================================================
    // Requests without usable credentials
    // =========================================================

    @Nested
    @DisplayName("without usable credentials: continues unauthenticated")
    class WithoutCredentials {

        @Test
        @DisplayName("no Authorization header")
        void missingHeader() throws Exception {
            when(request.getHeader("Authorization")).thenReturn(null);

            jwtFilter.doFilterInternal(request, response, filterChain);

            assertContinuesUnauthenticated();
            verifyNoInteractions(jwtUtil, userRepository, userSessionService);
        }

        @Test
        @DisplayName("a scheme that is not Bearer")
        void notBearer() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

            jwtFilter.doFilterInternal(request, response, filterChain);

            assertContinuesUnauthenticated();
            verifyNoInteractions(jwtUtil, userRepository, userSessionService);
        }

        @Test
        @DisplayName("token that does not validate (bad signature, expired, malformed)")
        void invalidToken() throws Exception {
            givenBearer("bad-token");
            when(jwtUtil.validateToken("bad-token")).thenReturn(false);

            jwtFilter.doFilterInternal(request, response, filterChain);

            assertContinuesUnauthenticated();
            verifyNoInteractions(userRepository, userSessionService);
        }

        @Test
        @DisplayName("valid signature but sub is not a UUID (token from before the change): never a 500")
        void subjectIsNotAUuid() throws Exception {
            givenBearer("legacy");
            when(jwtUtil.validateToken("legacy")).thenReturn(true);
            when(jwtUtil.extractUserId("legacy")).thenThrow(new IllegalArgumentException("not a UUID"));
            lenient().when(jwtUtil.extractSessionId("legacy")).thenReturn(sessionId);

            jwtFilter.doFilterInternal(request, response, filterChain);

            assertContinuesUnauthenticated();
            verifyNoInteractions(userRepository, userSessionService);
        }

        @Test
        @DisplayName("valid signature but no usable sid claim")
        void sessionIdMissing() throws Exception {
            givenBearer("no-sid");
            when(jwtUtil.validateToken("no-sid")).thenReturn(true);
            lenient().when(jwtUtil.extractUserId("no-sid")).thenReturn(userId);
            when(jwtUtil.extractSessionId("no-sid")).thenThrow(new IllegalArgumentException("no sid"));

            jwtFilter.doFilterInternal(request, response, filterChain);

            assertContinuesUnauthenticated();
            verifyNoInteractions(userRepository, userSessionService);
        }
    }

    // =========================================================
    // Valid token, but the account or session says no
    // =========================================================

    @Nested
    @DisplayName("valid token, rejected by the database state")
    class RejectedByState {

        @Test
        @DisplayName("401 – the user no longer exists")
        void userDoesNotExist() throws Exception {
            givenValidToken();
            givenUserAndSession(null, true);

            jwtFilter.doFilterInternal(request, response, filterChain);

            assertRejectedWith401();
        }

        @Test
        @DisplayName("401 – the account is suspended, even with a live session")
        void userSuspended() throws Exception {
            givenValidToken();
            givenUserAndSession(user(UserStatus.SUSPENDED, Role.BUYER), true);

            jwtFilter.doFilterInternal(request, response, filterChain);

            assertRejectedWith401();
        }

        @Test
        @DisplayName("revoked or expired session: treated as a stale credential, continues unauthenticated")
        void sessionNotActive() throws Exception {
            givenValidToken();
            givenUserAndSession(user(UserStatus.ACTIVE, Role.BUYER), false);

            jwtFilter.doFilterInternal(request, response, filterChain);

            assertContinuesUnauthenticated();
        }

        @Test
        @DisplayName("the session is checked against the user in the token (sid of another user is not enough)")
        void sessionIsCheckedForThatUser() throws Exception {
            givenValidToken();
            givenUserAndSession(user(UserStatus.ACTIVE, Role.BUYER), false);

            jwtFilter.doFilterInternal(request, response, filterChain);

            verify(userSessionService).isActive(sessionId, userId);
        }
    }

    // =========================================================
    // Authenticated requests
    // =========================================================

    @Nested
    @DisplayName("valid token, active user and active session")
    class Authenticated {

        @Test
        @DisplayName("authenticates with a UserPrincipal and continues the chain")
        void authenticates() throws Exception {
            User user = user(UserStatus.ACTIVE, Role.ADMIN);
            givenValidToken();
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userSessionService.isActive(sessionId, userId)).thenReturn(true);

            jwtFilter.doFilterInternal(request, response, filterChain);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth).isNotNull();
            assertThat(auth.isAuthenticated()).isTrue();
            assertThat(auth.getPrincipal()).isInstanceOf(UserPrincipal.class);
            assertThat(((UserPrincipal) auth.getPrincipal()).user()).isSameAs(user);
            verify(filterChain).doFilter(request, response);
            verify(authenticationEntryPoint, never()).commence(any(), any(), any());
        }

        @Test
        @DisplayName("the authorities come from the database, never from the token's role claim")
        void rolesComeFromTheDatabase() throws Exception {
            givenValidToken();
            when(userRepository.findById(userId))
                    .thenReturn(Optional.of(user(UserStatus.ACTIVE, Role.BUYER)));
            when(userSessionService.isActive(sessionId, userId)).thenReturn(true);

            jwtFilter.doFilterInternal(request, response, filterChain);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth).isNotNull();
            assertThat(auth.getAuthorities())
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactly("ROLE_BUYER");
            verify(jwtUtil, never()).extractRole(any());
        }

        @ParameterizedTest(name = "account {0} may authenticate")
        @EnumSource(value = UserStatus.class, names = {"ACTIVE", "PENDING_VERIFICATION"})
        @DisplayName("PENDING_VERIFICATION can authenticate: it needs a session to verify its phone")
        void pendingAccountsAuthenticate(UserStatus status) throws Exception {
            givenValidToken();
            when(userRepository.findById(userId)).thenReturn(Optional.of(user(status, Role.BUYER)));
            when(userSessionService.isActive(sessionId, userId)).thenReturn(true);

            jwtFilter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("an already authenticated context is left untouched: no database lookups")
        void existingAuthenticationIsKept() throws Exception {
            Authentication existing = new UsernamePasswordAuthenticationToken("someone", null, java.util.List.of());
            SecurityContextHolder.getContext().setAuthentication(existing);
            givenValidToken();

            jwtFilter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(existing);
            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(userRepository, userSessionService);
        }
    }
}