
package com.artelier.api.mapper;

import com.artelier.api.dto.response.UserResponse;
import com.artelier.api.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(User user);
}
