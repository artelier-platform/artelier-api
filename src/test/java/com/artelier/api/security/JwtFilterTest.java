package com.artelier.api.security;

import com.artelier.api.entity.User;
import com.artelier.api.enums.Role;
import com.artelier.api.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtFilter jwtFilter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @BeforeEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldContinueWhenAuthorizationHeaderMissing()
            throws ServletException, IOException {

        when(request.getHeader("Authorization"))
                .thenReturn(null);

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void shouldReturn401WhenTokenIsInvalid()
            throws ServletException, IOException {

        when(request.getHeader("Authorization"))
                .thenReturn("Bearer bad-token");

        when(jwtUtil.validateToken("bad-token"))
                .thenReturn(false);

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(response)
                .setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        verify(filterChain, never())
                .doFilter(any(), any());
    }

    @Test
    void shouldReturn401WhenUserDoesNotExist()
            throws ServletException, IOException {

        when(request.getHeader("Authorization"))
                .thenReturn("Bearer valid");

        when(jwtUtil.validateToken("valid"))
                .thenReturn(true);

        when(jwtUtil.extractUsername("valid"))
                .thenReturn("test@mail.com");

        when(userRepository.findByEmail("test@mail.com"))
                .thenReturn(Optional.empty());

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(response)
                .setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        verify(filterChain, never())
                .doFilter(any(), any());
    }

    @Test
    void shouldReturn401WhenUserIsBanned()
            throws ServletException, IOException {

        User user = new User();
        user.setBanned(true);

        when(request.getHeader("Authorization"))
                .thenReturn("Bearer valid");

        when(jwtUtil.validateToken("valid"))
                .thenReturn(true);

        when(jwtUtil.extractUsername("valid"))
                .thenReturn("test@mail.com");

        when(userRepository.findByEmail("test@mail.com"))
                .thenReturn(Optional.of(user));

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(response)
                .setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        verify(filterChain, never())
                .doFilter(any(), any());
    }

    @Test
    void shouldAuthenticateValidUser()
            throws ServletException, IOException {

        User user = new User();
        user.setEmail("test@mail.com");
        user.setBanned(false);
        user.setRole(Role.ADMIN);

        when(request.getHeader("Authorization"))
                .thenReturn("Bearer valid");

        when(jwtUtil.validateToken("valid"))
                .thenReturn(true);

        when(jwtUtil.extractUsername("valid"))
                .thenReturn("test@mail.com");

        when(userRepository.findByEmail("test@mail.com"))
                .thenReturn(Optional.of(user));

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertNotNull(
                SecurityContextHolder.getContext().getAuthentication()
        );

        verify(filterChain)
                .doFilter(request, response);
    }
}
