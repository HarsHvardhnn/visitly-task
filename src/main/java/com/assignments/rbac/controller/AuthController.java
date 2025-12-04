package com.assignments.rbac.controller;

import com.assignments.rbac.dto.ApiResponse;
import com.assignments.rbac.dto.CurrentUserResponse;
import com.assignments.rbac.dto.LoginRequest;
import com.assignments.rbac.dto.LoginResponse;
import com.assignments.rbac.dto.UserRegistrationRequest;
import com.assignments.rbac.dto.UserResponse;
import com.assignments.rbac.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> registerUser(@Valid @RequestBody UserRegistrationRequest request) {
        UserResponse userResponse = userService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(userResponse));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> loginUser(@Valid @RequestBody LoginRequest request) {
        LoginResponse loginResponse = userService.loginUser(request);
        return ResponseEntity.ok(ApiResponse.success(loginResponse));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<CurrentUserResponse>> getCurrentUser() {
        CurrentUserResponse currentUser = userService.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(currentUser));
    }
}
