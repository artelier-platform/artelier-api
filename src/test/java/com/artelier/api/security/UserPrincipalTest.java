
package com.artelier.api.security;

import com.artelier.api.entity.User;
import com.artelier.api.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

class UserPrincipalTest {

    private User user;
    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setEmail("test@artelier.com");
        user.setPasswordHash("hashed-password");
        user.setRole(Role.BUYER);
        user.setBanned(false);

        userPrincipal = new UserPrincipal(user);
    }

    @Test
    void getUser_ShouldReturnOriginalUser() {
        // Act
        User result = userPrincipal.getUser();

        // Assert
        assertThat(result).isSameAs(user);
    }

    @Test
    void getAuthorities_ShouldReturnBuyerRole() {
        // Act
        Collection<? extends GrantedAuthority> authorities =
                userPrincipal.getAuthorities();

        // Assert
        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_BUYER");
    }

    @Test
    void getAuthorities_ShouldReturnAdminRole() {
        // Arrange
        user.setRole(Role.ADMIN);

        // Act
        Collection<? extends GrantedAuthority> authorities =
                userPrincipal.getAuthorities();

        // Assert
        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void getPassword_ShouldReturnPasswordHash() {
        // Act
        String result = userPrincipal.getPassword();

        // Assert
        assertThat(result)
                .isEqualTo("hashed-password");
    }

    @Test
    void getUsername_ShouldReturnUserEmail() {
        // Act
        String result = userPrincipal.getUsername();

        // Assert
        assertThat(result)
                .isEqualTo("test@artelier.com");
    }

    @Test
    void isAccountNonLocked_ShouldReturnTrue_WhenUserIsNotBanned() {
        // Arrange
        user.setBanned(false);

        // Act
        boolean result = userPrincipal.isAccountNonLocked();

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void isAccountNonLocked_ShouldReturnFalse_WhenUserIsBanned() {
        // Arrange
        user.setBanned(true);

        // Act
        boolean result = userPrincipal.isAccountNonLocked();

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void user_ShouldReturnOriginalUser() {
        // Act
        User result = userPrincipal.user();

        // Assert
        assertThat(result).isSameAs(user);
    }
}
