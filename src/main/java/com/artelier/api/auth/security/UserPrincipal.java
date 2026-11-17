package com.artelier.api.auth.security;

import com.artelier.api.user.entity.User;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Custom implementation of Spring Security's {@link UserDetails} interface,
 * wrapping the domain {@link User} entity to provide authentication credentials, roles,
 * and account status details to the security context.
 */
public record UserPrincipal(User user) implements UserDetails {

    /**
     * Compatibility method for existing code that expects a getter for the underlying user entity.
     *
     * @return the wrapped {@link User} entity
     */
    public User getUser() {
        return user;
    }

    /**
     * Returns the authorities granted to the user based on their assigned role,
     * prefixed with "ROLE_" to comply with Spring Security role-based access control.
     *
     * @return a collection containing the user's granted authority
     */
    @Override
    public @NonNull Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );
    }

    /**
     * Returns the secure password hash used to authenticate the user.
     *
     * @return the user's password hash
     */
    @Override
    public @NonNull String getPassword() {
        return user.getPasswordHash();
    }

    /**
     * Returns the username (or email identifier) used to authenticate the user.
     *
     * @return the username string
     */
    @Override
    public @NonNull String getUsername() {
        return user.getUsername();
    }

    /**
     * Indicates whether the user's account is unlocked based on their activity status.
     *
     * @return {@code true} if the account is not locked; {@code false} otherwise
     */
    @Override
    public boolean isAccountNonLocked() {
        return !user.isActive();
    }

    /**
     * Returns a customized string representation of the user principal
     * containing the username and assigned role for logging and debugging purposes.
     *
     * @return a formatted string representation of the principal
     */
    @Override
    public String toString() {
        return "UserPrincipal[email=" + getUsername()
                + ", role=" + user.getRole() + "]";
    }
}