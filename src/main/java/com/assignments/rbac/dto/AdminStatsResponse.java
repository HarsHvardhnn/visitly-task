package com.assignments.rbac.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatsResponse {
    
    private Long totalUsers;
    private Long totalRoles;
    private Long activeUsers;
    private List<UserDetailInfo> users;
    private LocalDateTime generatedAt;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserDetailInfo {
        private Long userId;
        private String name;
        private String username;
        private String email;
        private Set<RoleResponse> roles;
        private LocalDateTime lastLoginAt;
        private String loginStatus; 
        private LocalDateTime createdAt;
        private String createdBy;
    }
}