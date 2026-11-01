package com.artelier.api.config;

import com.artelier.api.security.CustomAccessDeniedHandler;
import com.artelier.api.security.CustomAuthenticationEntryPoint;
import com.artelier.api.security.JwtFilter;
import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Real security chain for @WebMvcTest slices. Tests still mock JwtUtil and UserRepository. */
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