package com.artelier.api.auth.service;

import com.artelier.api.auth.dto.request.LoginRequest;
import com.artelier.api.auth.dto.request.RegisterRequest;
import com.artelier.api.auth.dto.response.AuthResponse;
import com.artelier.api.user.dto.UserProfileResponse;

/**
 * Service contract defining core authentication operations, including user login,
 * registration, token refreshing, and session termination (logout).
 */
public interface AuthService {

    /**
     * Authenticates a user based on their login credentials and generates an authentication session response.
     *
     * @param loginRequest the DTO containing login credentials (e.g., username/email and password)
     * @return an {@link AuthResponse} containing access and refresh tokens along with session details
     */
    AuthResponse login(LoginRequest loginRequest);

    /**
     * Registers a new user account in the system and returns the newly created user profile details.
     *
     * @param registerRequest the DTO containing user registration details
     * @return a {@link UserProfileResponse} containing the profile details of the newly registered user
     */
    UserProfileResponse register(RegisterRequest registerRequest);

    /**
     * Refreshes an existing user session by validating a refresh token and issuing a new access token.
     *
     * @param refreshToken the active refresh token string
     * @return an {@link AuthResponse} containing the renewed token set
     */
    AuthResponse refresh(String refreshToken);

    /**
     * Terminates an active user session by invalidating the provided refresh token.
     *
     * @param refreshToken the refresh token associated with the session to be closed
     */
    void logout(String refreshToken);

}