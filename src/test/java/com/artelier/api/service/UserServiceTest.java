package com.artelier.api.service;

import com.artelier.api.dto.response.UserResponse;
import com.artelier.api.entity.User;
import com.artelier.api.exception.ArtelierException;
import com.artelier.api.mapper.UserMapper;
import com.artelier.api.repository.UserRepository;
import com.artelier.api.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

    private static final String PAGE_NEGATIVE_MESSAGE = "Page index cannot be negative";
    private static final String SIZE_RANGE_MESSAGE = "Page size must be between 1 and 100";

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

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

    // ---------------------------------------------------------------------
    // setBanned
    // ---------------------------------------------------------------------

    @Test
    void setBanned_success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.setBanned(userId, true);

        assertTrue(user.isBanned());
        verify(userRepository).save(user);
    }

    @Test
    void setBanned_unbanUser_success() {
        user.setBanned(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.setBanned(userId, false);

        assertFalse(user.isBanned());
        verify(userRepository).save(user);
    }

    @Test
    void setBanned_userNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        ArtelierException ex = assertThrows(ArtelierException.class,
                () -> userService.setBanned(userId, true));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("User not found", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    // ---------------------------------------------------------------------
    // listUsers - validación de parámetros
    // ---------------------------------------------------------------------

    @Test
    void listUsers_negativePage_throwsBadRequest() {
        ArtelierException ex = assertThrows(ArtelierException.class,
                () -> userService.listUsers(-1, 10));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals(PAGE_NEGATIVE_MESSAGE, ex.getMessage());
        verifyNoInteractions(userRepository, userMapper);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 101})
    void listUsers_invalidSize_throwsBadRequest(int size) {
        ArtelierException ex = assertThrows(ArtelierException.class,
                () -> userService.listUsers(0, size));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals(SIZE_RANGE_MESSAGE, ex.getMessage());
        verifyNoInteractions(userRepository, userMapper);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 100})
    void listUsers_boundarySizes_areAccepted(int size) {
        when(userRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());

        assertDoesNotThrow(() -> userService.listUsers(0, size));

        verify(userRepository).findAll(PageRequest.of(0, size));
    }

    // ---------------------------------------------------------------------
    // listUsers - comportamiento
    // ---------------------------------------------------------------------

    @Test
    void listUsers_passesRequestedPageAndSizeToRepository() {
        when(userRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());

        userService.listUsers(2, 15);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).findAll(captor.capture());
        assertEquals(PageRequest.of(2, 15), captor.getValue());
    }

    @Test
    void listUsers_mapsEachUserToResponse() {
        User first = buildUser("first@example.com", "First User");
        User second = buildUser("second@example.com", "Second User");
        UserResponse firstResponse = buildResponse(first);
        UserResponse secondResponse = buildResponse(second);

        Page<User> repositoryPage = new PageImpl<>(
                List.of(first, second), PageRequest.of(0, 10), 2);
        when(userRepository.findAll(any(Pageable.class))).thenReturn(repositoryPage);
        when(userMapper.toResponse(first)).thenReturn(firstResponse);
        when(userMapper.toResponse(second)).thenReturn(secondResponse);

        Page<UserResponse> result = userService.listUsers(0, 10);

        assertEquals(List.of(firstResponse, secondResponse), result.getContent());
        assertEquals(2, result.getTotalElements());
        verify(userMapper).toResponse(first);
        verify(userMapper).toResponse(second);
    }

    @Test
    void listUsers_emptyPage_returnsEmptyAndDoesNotUseMapper() {
        when(userRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());

        Page<UserResponse> result = userService.listUsers(0, 10);

        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());
        verifyNoInteractions(userMapper);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private User buildUser(String email, String fullName) {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setEmail(email);
        u.setFullName(fullName);
        u.setBanned(false);
        return u;
    }

    private UserResponse buildResponse(User u) {
        return new UserResponse(
                u.getId(),
                u.getEmail(),
                u.getFullName(),
                null,
                u.isBanned(),
                null
        );
    }
}
