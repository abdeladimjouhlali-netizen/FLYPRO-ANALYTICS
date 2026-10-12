package com.flypro.analytics.user.mapper;

import com.flypro.analytics.user.dto.UserResponse;
import com.flypro.analytics.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapping User entity ↔ DTO.
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "role", source = "role.name")
    UserResponse toResponse(User user);
}
