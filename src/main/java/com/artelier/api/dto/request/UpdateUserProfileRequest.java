package com.artelier.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request used to update the personal information of the authenticated user.
 *
 * <p>All fields are optional, allowing partial profile updates.
 * Fields omitted or set to null must remain unchanged.</p>
 *
 * <p>The username, email addresses, phone number, account status,
 * and assigned role cannot be modified through this request.</p>
 *
 * @param firstName      updated first name (maximum 58 characters)
 * @param lastName       updated last name (maximum 34 characters)
 * @param profileImageUrl updated profile image URL (maximum 500 characters)
 */
@Schema(description = "Request used to update the authenticated user's profile")
public record UpdateUserProfileRequest(

        @Schema(
                description = "Updated first name",
                example = "Geronimo",
                maxLength = 58
        )
        @Size(min = 1, max = 58)
        @Pattern(regexp = ".*\\S.*")
        String firstName,

        @Schema(
                description = "Updated last name",
                example = "Rodriguez",
                maxLength = 34
        )
        @Size(min = 1, max = 34)
        @Pattern(regexp = ".*\\S.*")
        String lastName,

        @Schema(
                description = "Updated profile image URL",
                example = "https://res.cloudinary.com/example/image/upload/profile.jpg",
                maxLength = 500
        )
        @Size(max = 500)
        String profileImageUrl

) { }