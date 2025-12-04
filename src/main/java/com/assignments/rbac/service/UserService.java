package com.assignments.rbac.service;

import com.assignments.rbac.dto.CurrentUserResponse;
import com.assignments.rbac.dto.LoginRequest;
import com.assignments.rbac.dto.LoginResponse;
import com.assignments.rbac.dto.UserRegistrationRequest;
import com.assignments.rbac.dto.UserResponse;
import com.assignments.rbac.dto.events.UserLoginEvent;
import com.assignments.rbac.dto.events.UserRegistrationEvent;
import com.assignments.rbac.entity.User;
import com.assignments.rbac.exception.UserAlreadyExistsException;
import com.assignments.rbac.exception.UserNotFoundException;
import com.assignments.rbac.mapper.UserMapper;
import com.assignments.rbac.repository.UserRepository;
import com.assignments.rbac.repository.RoleRepository;
import com.assignments.rbac.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final EventPublisherService eventPublisherService;
    private final RequestInfoService requestInfoService;

    public UserResponse registerUser(UserRegistrationRequest request) {
        log.info("Attempting to register user with email: {}", request.getEmail());
        
        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("Registration failed - username already exists: {}", request.getUsername());
            throw new UserAlreadyExistsException("Username '" + request.getUsername() + "' is already taken");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed - email already exists: {}", request.getEmail());
            throw new UserAlreadyExistsException("Email '" + request.getEmail() + "' is already in use");
        }

        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        User savedUser = userRepository.save(user);
        log.info("User registered successfully with ID: {} and email: {}", savedUser.getId(), savedUser.getEmail());
        
        publishRegistrationEvent(savedUser);
        
        return userMapper.toResponse(savedUser);
    }

    @Transactional
    @CacheEvict(value = "userCache", key = "#request.email")
    public LoginResponse loginUser(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());
        
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            log.debug("Authentication successful for email: {}", request.getEmail());

            String jwt = jwtUtils.generateJwtToken(authentication);

            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new UserNotFoundException("User not found with email: " + request.getEmail()));

            // Update last login timestamp
            LocalDateTime loginTime = java.time.LocalDateTime.now();
            user.setLastLoginAt(loginTime);
            userRepository.save(user);
            log.debug("Updated last login timestamp for user {}: {} (cache evicted)", user.getEmail(), loginTime);

            UserResponse userResponse = userMapper.toResponse(user);

            log.info("Login successful for user ID: {} with email: {}", user.getId(), user.getEmail());
            
            publishLoginEvent(user, loginTime, true, null);
            
            return new LoginResponse(jwt, userResponse);
            
        } catch (AuthenticationException e) {
            log.warn("Login failed for email: {} - Invalid credentials", request.getEmail());
            
            publishFailedLoginEvent(request.getEmail(), "Invalid credentials");
            
            throw new BadCredentialsException("Invalid email or password: " + request.getEmail());
        }
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "userCache", key = "#root.target.getCurrentUserEmail()")
    public CurrentUserResponse getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        
        log.info("Getting current user details for email: {} (cache miss)", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));

        user.setRoles(new java.util.HashSet<>(roleRepository.findRolesByUserId(user.getId())));
        
        log.info("Current user found with ID: {} and roles: {}", user.getId(), user.getRoles().size());
        return userMapper.toCurrentUserResponse(user);
    }

    
    public String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

    
    @CacheEvict(value = "userCache", key = "#email")
    public void evictUserCache(String email) {
        log.debug("Manually evicted cache for user: {}", email);
    }

    @CacheEvict(value = "userCache", allEntries = true)
    public void evictAllUserCache() {
        log.debug("Cleared all user cache entries");
    }

    private void publishRegistrationEvent(User user) {
        try {
            UserRegistrationEvent event = new UserRegistrationEvent(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getName(),
                user.getCreatedAt(),
                requestInfoService.getClientIpAddress(),
                requestInfoService.getUserAgent()
            );
            
            eventPublisherService.publishUserRegistrationEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish registration event for user: {} - Error: {}", user.getEmail(), e.getMessage());
        }
    }

    private void publishLoginEvent(User user, LocalDateTime loginTime, boolean successful, String failureReason) {
        try {
            user.setRoles(new java.util.HashSet<>(roleRepository.findRolesByUserId(user.getId())));
            
            UserLoginEvent event = new UserLoginEvent(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getName(),
                user.getRoles().stream().map(role -> role.getName()).collect(Collectors.toList()),
                loginTime,
                requestInfoService.getClientIpAddress(),
                requestInfoService.getUserAgent(),
                successful,
                failureReason
            );
            
            eventPublisherService.publishUserLoginEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish login event for user: {} - Error: {}", user.getEmail(), e.getMessage());
        }
    }

    private void publishFailedLoginEvent(String email, String failureReason) {
        try {
            UserLoginEvent event = new UserLoginEvent(
                null, 
                null, 
                email,
                null, 
                null, 
                LocalDateTime.now(),
                requestInfoService.getClientIpAddress(),
                requestInfoService.getUserAgent(),
                false,
                failureReason
            );
            
            eventPublisherService.publishUserLoginEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish failed login event for email: {} - Error: {}", email, e.getMessage());
        }
    }
}
