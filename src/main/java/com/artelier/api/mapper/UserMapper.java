
package com.artelier.api.mapper;

import com.artelier.api.dto.response.UserEmailResponse;
import com.artelier.api.dto.response.UserProfileResponse;
import com.artelier.api.entity.User;
import com.artelier.api.entity.UserEmail;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserProfileResponse toProfileResponse(User user);
    UserEmailResponse toEmailResponse(UserEmail email);
}
