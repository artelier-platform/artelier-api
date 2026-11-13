package com.artelier.api.config;

import com.artelier.api.user.repository.UserRepository;
import com.artelier.api.auth.security.UserJwtService;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class SecurityMocksTestConfig {

    @Bean
    UserJwtService UserJwtService() {
        return Mockito.mock(UserJwtService.class);
    }

    @Bean
    UserRepository userRepository() {
        return Mockito.mock(UserRepository.class);
    }
}