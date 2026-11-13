package com.artelier.api.user.service;

import com.artelier.api.auth.dto.RegisterRequest;
import com.artelier.api.user.dto.UpdateUserProfileRequest;
import com.artelier.api.user.dto.UserProfileResponse;
import com.artelier.api.user.entity.User;
import com.artelier.api.user.enums.Role;
import com.artelier.api.user.enums.UserStatus;
import com.artelier.api.shared.exception.ArtelierException;
import com.artelier.api.user.mapper.UserMapper;
import com.artelier.api.user.repository.UserRepository;
import com.artelier.api.user.service.impl.UserServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Contract of UserService (phase 1: identity).
 * Error tests assert the HTTP status only; messages are your call.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService")
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserMapper userMapper;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private UserServiceImpl userService;

    // =========================================================
    // Fixtures
    // =========================================================

    private static RegisterRequest registerRequest(String username) {
        return new RegisterRequest(
                username, "StrongPass123", "Geronimo", "Rodriguez",
                "geronimo@example.com", "+57", "3001234567");
    }

    private static User userWithStatus(UserStatus status) {
        return User.builder()
                .id(UUID.randomUUID())
                .username("geronimo")
                .passwordHash("hash")
                .firstName("Geronimo")
                .lastName("Rodriguez")
                .phoneCountryCode("+57")
                .phoneNumber("3001234567")
                .role(Role.BUYER)
                .status(status)
                .build();
    }

    private static UserProfileResponse profileOf(User u) {
        return new UserProfileResponse(
                u.getId(), u.getUsername(), u.getFirstName(), u.getLastName(),
                u.getPhoneCountryCode(), u.getPhoneNumber(), null, null,
                u.getStatus(), u.getRole(), List.of(), Instant.now());
    }

    private static void assertStatus(Runnable action, HttpStatus expected) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(ArtelierException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(expected));
    }

    @Nested
    @DisplayName("getProfile")
    class GetProfile {

        @Test
        @DisplayName("returns the mapped profile")
        void returnsProfile() {
            User user = userWithStatus(UserStatus.ACTIVE);
            UserProfileResponse expected = profileOf(user);
            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
            when(userMapper.toProfileResponse(user)).thenReturn(expected);

            assertThat(userService.getProfile(user.getId())).isSameAs(expected);
        }

        @Test
        @DisplayName("404 – unknown user")
        void notFound() {
            UUID id = UUID.randomUUID();
            when(userRepository.findById(id)).thenReturn(Optional.empty());

            assertStatus(() -> userService.getProfile(id), HttpStatus.NOT_FOUND);
            verifyNoInteractions(userMapper);
        }
    }

    // =========================================================
    // updateProfile
    // =========================================================

    @Nested
    @DisplayName("updateProfile")
    class UpdateProfile {

        @Test
        @DisplayName("updates every provided field and returns the mapped profile")
        void updatesAllFields() {
            User user = userWithStatus(UserStatus.ACTIVE);
            UserProfileResponse expected = profileOf(user);
            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
            when(userMapper.toProfileResponse(user)).thenReturn(expected);

            UserProfileResponse result = userService.updateProfile(user.getId(),
                    new UpdateUserProfileRequest("Nuevo", "Apellido", "https://img.test/p.jpg"));

            assertThat(user.getFirstName()).isEqualTo("Nuevo");
            assertThat(user.getLastName()).isEqualTo("Apellido");
            assertThat(user.getProfileImageUrl()).isEqualTo("https://img.test/p.jpg");
            assertThat(result).isSameAs(expected);
        }

        @Test
        @DisplayName("null fields stay unchanged (partial update)")
        void nullFieldsAreUnchanged() {
            User user = userWithStatus(UserStatus.ACTIVE);
            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
            when(userMapper.toProfileResponse(user)).thenReturn(profileOf(user));

            userService.updateProfile(user.getId(),
                    new UpdateUserProfileRequest("Nuevo", null, null));

            assertThat(user.getFirstName()).isEqualTo("Nuevo");
            assertThat(user.getLastName()).isEqualTo("Rodriguez");
            assertThat(user.getProfileImageUrl()).isNull();
        }

        @Test
        @DisplayName("never changes username, role, status or password")
        void protectedFieldsAreUntouched() {
            User user = userWithStatus(UserStatus.ACTIVE);
            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
            when(userMapper.toProfileResponse(user)).thenReturn(profileOf(user));

            userService.updateProfile(user.getId(),
                    new UpdateUserProfileRequest("Nuevo", "Apellido", null));

            assertThat(user.getUsername()).isEqualTo("geronimo");
            assertThat(user.getRole()).isEqualTo(Role.BUYER);
            assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(user.getPasswordHash()).isEqualTo("hash");
        }

        @Test
        @DisplayName("a PENDING_VERIFICATION account can still edit its profile")
        void pendingAccountCanUpdate() {
            User user = userWithStatus(UserStatus.PENDING_VERIFICATION);
            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
            when(userMapper.toProfileResponse(user)).thenReturn(profileOf(user));

            userService.updateProfile(user.getId(),
                    new UpdateUserProfileRequest("Nuevo", null, null));

            assertThat(user.getFirstName()).isEqualTo("Nuevo");
        }

        @Test
        @DisplayName("403 – suspended account is restricted and nothing changes")
        void suspendedAccountIsRejected() {
            User user = userWithStatus(UserStatus.SUSPENDED);
            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

            assertStatus(() -> userService.updateProfile(user.getId(),
                    new UpdateUserProfileRequest("Nuevo", null, null)), HttpStatus.FORBIDDEN);

            assertThat(user.getFirstName()).isEqualTo("Geronimo");
        }

        @Test
        @DisplayName("404 – unknown user")
        void notFound() {
            UUID id = UUID.randomUUID();
            when(userRepository.findById(id)).thenReturn(Optional.empty());

            assertStatus(() -> userService.updateProfile(id,
                    new UpdateUserProfileRequest("Nuevo", null, null)), HttpStatus.NOT_FOUND);
        }
    }

    // =========================================================
    // suspend
    // =========================================================

    @Nested
    @DisplayName("suspend")
    class Suspend {

        @ParameterizedTest(name = "{0} → SUSPENDED")
        @ValueSource(strings = {"ACTIVE", "PENDING_VERIFICATION"})
        void suspendsUser(String from) {
            User user = userWithStatus(UserStatus.valueOf(from));
            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

            userService.suspend(user.getId());

            assertThat(user.getStatus()).isEqualTo(UserStatus.SUSPENDED);
        }

        @Test
        @DisplayName("suspending an already suspended user is idempotent")
        void idempotent() {
            User user = userWithStatus(UserStatus.SUSPENDED);
            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

            userService.suspend(user.getId());

            assertThat(user.getStatus()).isEqualTo(UserStatus.SUSPENDED);
        }

        @Test
        @DisplayName("404 – unknown user")
        void notFound() {
            UUID id = UUID.randomUUID();
            when(userRepository.findById(id)).thenReturn(Optional.empty());

            assertStatus(() -> userService.suspend(id), HttpStatus.NOT_FOUND);
        }
    }

    // =========================================================
    // listUsers (kept from the previous suite, adapted to the new DTO)
    // =========================================================

    @Nested
    @DisplayName("listUsers")
    class ListUsers {

        @Test
        void negativePageIsRejected() {
            assertStatus(() -> userService.listUsers(-1, 10), HttpStatus.BAD_REQUEST);
            verifyNoInteractions(userRepository, userMapper);
        }

        @ParameterizedTest(name = "size {0} is rejected")
        @ValueSource(ints = {0, -1, 101})
        void invalidSizeIsRejected(int size) {
            assertStatus(() -> userService.listUsers(0, size), HttpStatus.BAD_REQUEST);
            verifyNoInteractions(userRepository, userMapper);
        }

        @ParameterizedTest(name = "size {0} is accepted")
        @ValueSource(ints = {1, 100})
        void boundarySizesAreAccepted(int size) {
            when(userRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());

            userService.listUsers(0, size);

            verify(userRepository).findAll(PageRequest.of(0, size));
        }

        @Test
        void mapsEachUserToProfile() {
            User first = userWithStatus(UserStatus.ACTIVE);
            User second = userWithStatus(UserStatus.SUSPENDED);
            UserProfileResponse firstResponse = profileOf(first);
            UserProfileResponse secondResponse = profileOf(second);

            when(userRepository.findAll(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(first, second), PageRequest.of(0, 10), 2));
            when(userMapper.toProfileResponse(first)).thenReturn(firstResponse);
            when(userMapper.toProfileResponse(second)).thenReturn(secondResponse);

            Page<UserProfileResponse> result = userService.listUsers(0, 10);

            assertThat(result.getContent()).containsExactly(firstResponse, secondResponse);
            assertThat(result.getTotalElements()).isEqualTo(2);
        }
    }
}