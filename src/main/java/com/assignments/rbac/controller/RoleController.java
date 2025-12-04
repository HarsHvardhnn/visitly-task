package com.assignments.rbac.controller;

import com.assignments.rbac.dto.ApiResponse;
import com.assignments.rbac.dto.AssignRoleRequest;
import com.assignments.rbac.dto.RoleRequest;
import com.assignments.rbac.dto.RoleResponse;
import com.assignments.rbac.dto.UserResponse;
import com.assignments.rbac.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RoleResponse>> createRole(@Valid @RequestBody RoleRequest request) {
        RoleResponse roleResponse = roleService.createRole(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(roleResponse));
    }

    @PostMapping("/users/{userId}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> assignRolesToUser(
            @PathVariable Long userId,
            @Valid @RequestBody AssignRoleRequest request) {
        UserResponse userResponse = roleService.assignRolesToUser(userId, request);
        return ResponseEntity.ok(ApiResponse.success(userResponse));
    }
}

