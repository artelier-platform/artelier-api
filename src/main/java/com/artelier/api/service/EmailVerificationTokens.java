package com.artelier.api.service;

import java.util.UUID;

/**
 * Service contract for issuing and parsing secure JSON Web Tokens (JWT)
 * used during the user email address verification process.
 */
public interface EmailVerificationTokens {

    /**
     * Issues a signed verification token for the specified email record identifier.
     *
     * @param emailId the unique identifier of the email record to be verified
     * @return the compact signed JWT verification token string
     */
    String issue(UUID emailId);

    /**
     * Parses and validates a JWT verification token, extracting the associated email record identifier.
     *
     * @param token the JWT verification token string
     * @return the extracted email record {@link UUID}
     * @throws io.jsonwebtoken.JwtException if the token is invalid, expired, or malformed
     */
    UUID parse(String token);
}