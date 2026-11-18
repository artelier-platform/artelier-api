package com.artelier.api.shared.config;

import com.artelier.api.auth.security.CustomAccessDeniedHandler;
import com.artelier.api.auth.security.CustomAuthenticationEntryPoint;
import com.artelier.api.auth.security.JwtFilter;
import com.artelier.api.auth.util.SecurityRoutes;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration class defining the security filter chain,
 * authorization rules, exception handling, and stateless session policies.
 */
@Slf4j
@AllArgsConstructor
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;

    /**
     * Configures the security filter chain for HTTP security requests.
     *
     * @param http the {@link HttpSecurity} builder to configure
     * @return the built {@link SecurityFilterChain}
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
                .cors(Customizer.withDefaults())

                .csrf(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(SecurityRoutes.PUBLIC_ROUTES.toArray(String[]::new)).permitAll()
                        .requestMatchers(HttpMethod.GET, SecurityRoutes.PUBLIC_GET_ROUTES.toArray(String[]::new)).permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )

                .addFilterBefore(jwtFilter,
                        UsernamePasswordAuthenticationFilter.class)

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                );

        return http.build();
    }
}