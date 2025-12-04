package com.assignments.rbac.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
public class UserResponse {

    private Long id;
    private String name;
    private String username;
    private String email;
    private String createdBy;
    private LocalDateTime createdAt;
    private String lastUpdatedBy;
    private LocalDateTime lastUpdatedAt;
    private Set<RoleResponse> roles;
}

