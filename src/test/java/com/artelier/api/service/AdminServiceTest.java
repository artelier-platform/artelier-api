package com.artelier.api.service;

import com.artelier.api.entity.User;
import com.artelier.api.exception.ArtelierException;
import com.artelier.api.repository.UserRepository;
import com.artelier.api.service.impl.AdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminServiceImpl adminService;

    private UUID userId;
    private User user;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        userId = UUID.randomUUID();
        user = new User();
        user.setId(userId);
        user.setBanned(false);
    }

    @Test
    void setBanned_success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        adminService.setBanned(userId, true);

        assertTrue(user.isBanned());
        verify(userRepository).save(user);
    }

    @Test
    void setBanned_userNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ArtelierException.class,
                () -> adminService.setBanned(userId, true));

        verify(userRepository, never()).save(any());
    }
}