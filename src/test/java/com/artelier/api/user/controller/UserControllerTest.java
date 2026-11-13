package com.artelier.api.user.controller;

import com.artelier.api.config.WithSecurity;
import com.artelier.api.user.dto.UpdateUserProfileRequest;
import com.artelier.api.user.dto.UserProfileResponse;
import com.artelier.api.user.entity.User;
import com.artelier.api.user.enums.Role;
import com.artelier.api.user.enums.UserStatus;
import com.artelier.api.shared.exception.ArtelierException;
import com.artelier.api.auth.security.UserPrincipal;
import com.artelier.api.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@WithSecurity
@DisplayName("UserController")
class UserControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private UserService userService;

    // =========================================================
    // Fixtures
    // =========================================================

    private static final UUID OWNER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID OTHER_ID = UUID.fromString("7c9e6679-7425-40de-944b-e07fc1f90ae7");
    private static final String PROFILE_URL = "/users/{userId}/profile";

    private static User user(UUID id, String username, Role role) {
        return User.builder()
                .id(id).username(username).passwordHash("hash")
                .firstName("Geronimo").lastName("Rodriguez")
                .phoneCountryCode("+57").phoneNumber("3001234567")
                .role(role).status(UserStatus.ACTIVE)
                .build();
    }

    private static RequestPostProcessor authenticatedAs(User user) {
        UserPrincipal principal = new UserPrincipal(user);
        return authentication(new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()));
    }

    private static RequestPostProcessor asOwner() {
        return authenticatedAs(user(OWNER_ID, "owner", Role.BUYER));
    }

    private static RequestPostProcessor asOtherBuyer() {
        return authenticatedAs(user(OTHER_ID, "other", Role.BUYER));
    }

    private static RequestPostProcessor asAdmin() {
        return authenticatedAs(user(UUID.randomUUID(), "admin", Role.ADMIN));
    }

    /** Instants are null on purpose: these tests are about HTTP, not date serialization. */
    private static UserProfileResponse profileOf() {
        return new UserProfileResponse(UserControllerTest.OWNER_ID, "owner", "Geronimo", "Rodriguez", "+57", "3001234567",
                null, null, UserStatus.ACTIVE, Role.BUYER, List.of(), null);
    }

    private static String updateJson() {
        return """
                { "firstName": "%s", "lastName": "%s", "profileImageUrl": "%s" }
                """.formatted("Nuevo", "Apellido", "https://img.test/p.jpg");
    }

    // Each Argument: test name, request body, field expected in the validation errors.
    static Stream<Arguments> invalidUpdateBodies() {
        return Stream.of(
                Arguments.of("firstName blank", "{ \"firstName\": \" \" }", "firstName"),
                Arguments.of("firstName longer than 58",
                        "{ \"firstName\": \"" + "a".repeat(59) + "\" }", "firstName"),
                Arguments.of("lastName blank", "{ \"lastName\": \" \" }", "lastName"),
                Arguments.of("lastName longer than 34",
                        "{ \"lastName\": \"" + "a".repeat(35) + "\" }", "lastName"),
                Arguments.of("profileImageUrl longer than 500",
                        "{ \"profileImageUrl\": \"" + "a".repeat(501) + "\" }", "profileImageUrl")
        );
    }

    // =========================================================
    // GET /users/{userId}/profile
    // =========================================================

    @Nested
    @DisplayName("GET /users/{userId}/profile")
    class GetProfile {

        @Test
        @DisplayName("200 – the owner reads their own profile")
        void owner() throws Exception {
            when(userService.getProfile(OWNER_ID)).thenReturn(profileOf());

            mockMvc.perform(get(PROFILE_URL, OWNER_ID).with(asOwner()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("User profile retrieved successfully"))
                    .andExpect(jsonPath("$.data.id").value(OWNER_ID.toString()))
                    .andExpect(jsonPath("$.data.username").value("owner"))
                    .andExpect(jsonPath("$.data.passwordHash").doesNotExist());

            verify(userService).getProfile(OWNER_ID);
        }

        @Test
        @DisplayName("200 – an ADMIN can read any profile")
        void admin() throws Exception {
            when(userService.getProfile(OWNER_ID)).thenReturn(profileOf());

            mockMvc.perform(get(PROFILE_URL, OWNER_ID).with(asAdmin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(OWNER_ID.toString()));
        }

        @Test
        @DisplayName("403 – another BUYER cannot read it, and the service is never called")
        void otherBuyer() throws Exception {
            mockMvc.perform(get(PROFILE_URL, OWNER_ID).with(asOtherBuyer()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false));

            verifyNoInteractions(userService);
        }

        @Test
        @DisplayName("401 – unauthenticated request")
        void unauthenticated() throws Exception {
            mockMvc.perform(get(PROFILE_URL, OWNER_ID))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Authentication required"));

            verifyNoInteractions(userService);
        }

        @Test
        @DisplayName("400 – userId is not a UUID (never a 500)")
        void invalidUuid() throws Exception {
            mockMvc.perform(get(PROFILE_URL, "not-a-uuid").with(asOwner()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Invalid value for parameter 'userId'"));

            verifyNoInteractions(userService);
        }

        @Test
        @DisplayName("404 – the user does not exist")
        void notFound() throws Exception {
            when(userService.getProfile(OWNER_ID))
                    .thenThrow(ArtelierException.notFound("User not found"));

            mockMvc.perform(get(PROFILE_URL, OWNER_ID).with(asOwner()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("User not found"));
        }
    }

    // =========================================================
    // PUT /users/{userId}/profile
    // =========================================================

    @Nested
    @DisplayName("PUT /users/{userId}/profile")
    class UpdateProfile {

        @Test
        @DisplayName("200 – the owner updates their profile")
        void owner() throws Exception {
            UpdateUserProfileRequest expected = new UpdateUserProfileRequest(
                    "Nuevo", "Apellido", "https://img.test/p.jpg");
            when(userService.updateProfile(OWNER_ID, expected)).thenReturn(profileOf());

            mockMvc.perform(put(PROFILE_URL, OWNER_ID).with(asOwner())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateJson()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("User profile updated successfully"))
                    .andExpect(jsonPath("$.data.id").value(OWNER_ID.toString()));

            verify(userService).updateProfile(OWNER_ID, expected);
        }

        @Test
        @DisplayName("200 – empty body is a valid partial update (all fields null)")
        void emptyBody() throws Exception {
            UpdateUserProfileRequest expected = new UpdateUserProfileRequest(null, null, null);
            when(userService.updateProfile(OWNER_ID, expected)).thenReturn(profileOf());

            mockMvc.perform(put(PROFILE_URL, OWNER_ID).with(asOwner())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk());

            verify(userService).updateProfile(OWNER_ID, expected);
        }

        @Test
        @DisplayName("403 – an ADMIN cannot edit someone else's personal data")
        void adminCannotEdit() throws Exception {
            mockMvc.perform(put(PROFILE_URL, OWNER_ID).with(asAdmin())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateJson()))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(userService);
        }

        @Test
        @DisplayName("403 – another BUYER cannot edit it")
        void otherBuyer() throws Exception {
            mockMvc.perform(put(PROFILE_URL, OWNER_ID).with(asOtherBuyer())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateJson()))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(userService);
        }

        @Test
        @DisplayName("401 – unauthenticated request")
        void unauthenticated() throws Exception {
            mockMvc.perform(put(PROFILE_URL, OWNER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Authentication required"));

            verifyNoInteractions(userService);
        }

        @ParameterizedTest(name = "400 – {0}")
        @MethodSource("com.artelier.api.user.controller.UserControllerTest#invalidUpdateBodies")
        @DisplayName("400 – invalid body is rejected before reaching the service")
        void invalidBody(String name, String body, String field) throws Exception {
            mockMvc.perform(put(PROFILE_URL, OWNER_ID).with(asOwner())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Validation failed"))
                    .andExpect(jsonPath("$.data[?(@.field == '" + field + "')]").isNotEmpty());

            verifyNoInteractions(userService);
        }

        @Test
        @DisplayName("400 – malformed JSON")
        void malformedJson() throws Exception {
            mockMvc.perform(put(PROFILE_URL, OWNER_ID).with(asOwner())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ \"firstName\": "))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Invalid request body. Please check JSON format."));

            verifyNoInteractions(userService);
        }

        @Test
        @DisplayName("400 – userId is not a UUID")
        void invalidUuid() throws Exception {
            mockMvc.perform(put(PROFILE_URL, "not-a-uuid").with(asOwner())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Invalid value for parameter 'userId'"));

            verifyNoInteractions(userService);
        }

        @Test
        @DisplayName("403 – suspended account (the service rejects it)")
        void suspendedAccount() throws Exception {
            when(userService.updateProfile(OWNER_ID, new UpdateUserProfileRequest("Nuevo", null, null)))
                    .thenThrow(ArtelierException.forbidden("Account is suspended"));

            mockMvc.perform(put(PROFILE_URL, OWNER_ID).with(asOwner())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ \"firstName\": \"Nuevo\" }"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("Account is suspended"));
        }

        @Test
        @DisplayName("404 – the user does not exist")
        void notFound() throws Exception {
            when(userService.updateProfile(OWNER_ID, new UpdateUserProfileRequest("Nuevo", null, null)))
                    .thenThrow(ArtelierException.notFound("User not found"));

            mockMvc.perform(put(PROFILE_URL, OWNER_ID).with(asOwner())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ \"firstName\": \"Nuevo\" }"))
                    .andExpect(status().isNotFound());
        }
    }
}