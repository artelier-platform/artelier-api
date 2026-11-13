package com.artelier.api.auth.security;

import com.artelier.api.user.entity.User;
import com.artelier.api.user.enums.Role;
import com.artelier.api.user.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.security.core.GrantedAuthority;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserPrincipal")
class UserPrincipalTest {

    private User user;
    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .username("Geronimo01")
                .passwordHash("hashed-password")
                .role(Role.BUYER)
                .status(UserStatus.ACTIVE)
                .build();
        principal = new UserPrincipal(user);
    }

    @Test
    @DisplayName("exposes the wrapped user through user() and getUser()")
    void exposesUser() {
        assertThat(principal.user()).isSameAs(user);
        assertThat(principal.getUser()).isSameAs(user);
    }

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({"BUYER, ROLE_BUYER", "ADMIN, ROLE_ADMIN"})
    @DisplayName("the single authority is the role with the ROLE_ prefix")
    void authorities(Role role, String expected) {
        user.setRole(role);

        assertThat(principal.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly(expected);
    }

    @Test
    @DisplayName("password is the stored hash")
    void password() {
        assertThat(principal.getPassword()).isEqualTo("hashed-password");
    }

    @Test
    @DisplayName("username is the account username, not an email")
    void username() {
        assertThat(principal.getUsername()).isEqualTo("Geronimo01");
    }

    @ParameterizedTest(name = "{0} → non locked: {1}")
    @CsvSource({
            "ACTIVE,               true",
            "PENDING_VERIFICATION, true",
            "SUSPENDED,            false"
    })
    @DisplayName("only a SUSPENDED account is locked")
    void accountNonLocked(UserStatus status, boolean expected) {
        user.setStatus(status);

        assertThat(principal.isAccountNonLocked()).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0} is enabled")
    @EnumSource(value = UserStatus.class, names = {"ACTIVE", "PENDING_VERIFICATION"})
    @DisplayName("accounts that may authenticate are enabled")
    void enabled(UserStatus status) {
        user.setStatus(status);

        assertThat(principal.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("toString identifies the account but never leaks the password hash")
    void toStringDoesNotLeak() {
        assertThat(principal.toString())
                .contains("Geronimo01")
                .contains("BUYER")
                .doesNotContain("hashed-password");
    }
}