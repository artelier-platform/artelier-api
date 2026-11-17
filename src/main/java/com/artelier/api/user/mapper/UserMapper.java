package com.artelier.api.user.mapper;

import com.artelier.api.user.dto.UserEmailResponse;
import com.artelier.api.user.dto.UserProfileResponse;
import com.artelier.api.user.entity.User;
import com.artelier.api.user.entity.UserEmail;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper interface responsible for converting user domain entities 
 * and their associated details into corresponding response data transfer objects.
 *
 * <p>Configured with Spring component model integration for automatic dependency injection.</p>
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    /**
     * Converts a {@link User} entity into a {@link UserProfileResponse} DTO.
     *
     * @param user the persistent user entity to be mapped
     * @return the corresponding {@link UserProfileResponse} data transfer object
     */
    UserProfileResponse toProfileResponse(User user);

    /**
     * Converts a {@link UserEmail} entity into a {@link UserEmailResponse} DTO.
     *
     * @param email the persistent user email entity to be mapped
     * @return the corresponding {@link UserEmailResponse} data transfer object
     */
    UserEmailResponse toEmailResponse(UserEmail email);
}