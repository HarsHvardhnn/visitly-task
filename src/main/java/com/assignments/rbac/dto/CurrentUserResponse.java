package com.assignments.rbac.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
public class CurrentUserResponse {

    private Long id;
    private String name;
    private String username;
    private String email;
    private Set<RoleResponse> roles;
    private String createdBy;
    private LocalDateTime createdAt;
    private String lastUpdatedBy;
    private LocalDateTime lastUpdatedAt;
}

