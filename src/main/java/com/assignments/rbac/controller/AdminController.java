package com.assignments.rbac.controller;

import com.assignments.rbac.dto.AdminStatsResponse;
import com.assignments.rbac.dto.ApiResponse;
import com.assignments.rbac.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminStatsResponse>> getAdminStats() {
        log.info("Admin stats endpoint accessed");
        
        AdminStatsResponse stats = adminService.getAdminStats();
        
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}