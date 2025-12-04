package com.assignments.rbac.service;

import com.assignments.rbac.dto.AdminStatsResponse;
import com.assignments.rbac.entity.User;
import com.assignments.rbac.mapper.RoleMapper;
import com.assignments.rbac.repository.RoleRepository;
import com.assignments.rbac.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;

    public AdminStatsResponse getAdminStats() {
        log.info("Generating admin statistics");
        
        long totalUsers = userRepository.count();
        long totalRoles = roleRepository.count();
        
        List<AdminStatsResponse.UserDetailInfo> users = getAllUsersWithDetails();
        
        long activeUsers = users.stream()
                .filter(user -> user.getLastLoginAt() != null)
                .count();

        AdminStatsResponse stats = new AdminStatsResponse(
                totalUsers,
                totalRoles,
                activeUsers,
                users,
                LocalDateTime.now()
        );

        log.info("Admin stats generated: {} total users, {} total roles, {} active users", 
                totalUsers, totalRoles, activeUsers);
        
        return stats;
    }

    private List<AdminStatsResponse.UserDetailInfo> getAllUsersWithDetails() {
        List<User> allUsers = userRepository.findAllUsersOrderByCreated();
        
        return allUsers.stream()
                .map(this::mapUserToDetailInfo)
                .collect(Collectors.toList());
    }

    private AdminStatsResponse.UserDetailInfo mapUserToDetailInfo(User user) {
        Set<com.assignments.rbac.entity.Role> userRoles = new HashSet<>(roleRepository.findRolesByUserId(user.getId()));
        
        String loginStatus = determineLoginStatus(user.getLastLoginAt());
        
        return new AdminStatsResponse.UserDetailInfo(
                user.getId(),
                user.getName(),
                user.getUsername(),
                user.getEmail(),
                userRoles.stream()
                        .map(roleMapper::toResponse)
                        .collect(Collectors.toSet()),
                user.getLastLoginAt(),
                loginStatus,
                user.getCreatedAt(),
                user.getCreatedBy()
        );
    }

    private String determineLoginStatus(LocalDateTime lastLoginAt) {
        if (lastLoginAt == null) {
            return "Never logged in";
        }
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime twentyFourHoursAgo = now.minusHours(24);
        
        if (lastLoginAt.isAfter(twentyFourHoursAgo)) {
            return "Active";
        } else {
            return "Inactive";
        }
    }
}