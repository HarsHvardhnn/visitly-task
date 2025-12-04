package com.assignments.rbac.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RoleResponse {

    private Long id;
    private String name;
    private String description;
    private String createdBy;
    private LocalDateTime createdAt;
    private String lastUpdatedBy;
    private LocalDateTime lastUpdatedAt;
}

