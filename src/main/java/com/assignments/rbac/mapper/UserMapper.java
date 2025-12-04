package com.assignments.rbac.mapper;

import com.assignments.rbac.dto.CurrentUserResponse;
import com.assignments.rbac.dto.UserRegistrationRequest;
import com.assignments.rbac.dto.UserResponse;
import com.assignments.rbac.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "lastUpdatedBy", ignore = true)
    @Mapping(target = "lastUpdatedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    User toEntity(UserRegistrationRequest request);

    UserResponse toResponse(User user);

    CurrentUserResponse toCurrentUserResponse(User user);
}
