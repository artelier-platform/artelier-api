package com.artelier.api.config;

import com.artelier.api.auth.security.CustomAccessDeniedHandler;
import com.artelier.api.auth.security.CustomAuthenticationEntryPoint;
import com.artelier.api.auth.security.JwtFilter;
import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import com.artelier.api.auth.security.UserJwtService;
import com.artelier.api.shared.config.SecurityConfig;
import com.artelier.api.user.repository.UserRepository;

/** Real security chain for @WebMvcTest slices. Tests still mock UserJwtService and UserRepository. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({
        SecurityConfig.class,
        JacksonTestConfig.class,
        SecurityMocksTestConfig.class,
        JwtFilter.class,
        CustomAuthenticationEntryPoint.class,
        CustomAccessDeniedHandler.class
})
public @interface WithSecurity {}