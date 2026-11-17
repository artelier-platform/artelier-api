package com.artelier.api.user.mapper;

import com.artelier.api.user.dto.UserEmailResponse;
import com.artelier.api.user.entity.UserEmail;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper interface responsible for converting between {@link UserEmail} entities
 * and their corresponding response data transfer objects ({@link UserEmailResponse}).
 *
 * <p>Configured with Spring component model integration for automatic dependency injection.</p>
 */
@Mapper(componentModel = "spring")
public interface UserEmailMapper {

    /**
     * Converts a {@link UserEmail} entity into a {@link UserEmailResponse} DTO.
     *
     * @param userEmail the persistent user email entity to be mapped
     * @return the corresponding {@link UserEmailResponse} data transfer object
     */
    UserEmailResponse toResponse(UserEmail userEmail);
}