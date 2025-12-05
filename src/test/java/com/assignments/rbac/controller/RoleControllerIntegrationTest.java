package com.assignments.rbac.controller;

import com.assignments.rbac.dto.AssignRoleRequest;
import com.assignments.rbac.dto.RoleRequest;
import com.assignments.rbac.dto.UserRegistrationRequest;
import com.assignments.rbac.entity.Role;
import com.assignments.rbac.entity.User;
import com.assignments.rbac.repository.RoleRepository;
import com.assignments.rbac.repository.UserRepository;
import com.assignments.rbac.security.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Set;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = com.assignments.rbac.RbacApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class RoleControllerIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    private User adminUser;
    private User regularUser;
    private String adminToken;
    private String userToken;
    private Role adminRole;
    private Role userRole;

    @BeforeEach
    void setUp() {
        System.out.println("=== Test Setup Starting ===");
        
        try {
            // Clean up repositories in correct order (users first due to foreign keys)
            System.out.println("Cleaning up users...");
            userRepository.deleteAll();
            System.out.println("Cleaning up roles...");
            roleRepository.deleteAll();
            System.out.println("Cleanup completed successfully");
        } catch (Exception e) {
            // If cleanup fails, continue - might be due to constraints
            System.out.println("Cleanup warning: " + e.getMessage());
            e.printStackTrace();
        }

        try {
            // Create or find existing roles
            System.out.println("Creating ADMIN role...");
            adminRole = roleRepository.findByName("ADMIN")
                    .orElseGet(() -> {
                        Role role = new Role();
                        role.setName("ADMIN");
                        role.setDescription("Administrator role");
                        Role saved = roleRepository.save(role);
                        System.out.println("Created ADMIN role with ID: " + saved.getId());
                        return saved;
                    });

            System.out.println("Creating USER role...");
            userRole = roleRepository.findByName("USER")
                    .orElseGet(() -> {
                        Role role = new Role();
                        role.setName("USER");
                        role.setDescription("Regular user role");
                        Role saved = roleRepository.save(role);
                        System.out.println("Created USER role with ID: " + saved.getId());
                        return saved;
                    });
        } catch (Exception e) {
            System.out.println("Error creating roles: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }

        try {
            // Create admin user
            System.out.println("Creating admin user...");
            adminUser = new User();
            adminUser.setName("Admin User");
            adminUser.setUsername("admin");
            adminUser.setEmail("admin@test.com");
            adminUser.setPassword(passwordEncoder.encode("password123"));
            adminUser.setRoles(Set.of(adminRole));
            adminUser = userRepository.save(adminUser);
            System.out.println("Created admin user with ID: " + adminUser.getId());

            // Create regular user
            System.out.println("Creating regular user...");
            regularUser = new User();
            regularUser.setName("Regular User");
            regularUser.setUsername("user");
            regularUser.setEmail("user@test.com");
            regularUser.setPassword(passwordEncoder.encode("password123"));
            regularUser.setRoles(Set.of(userRole));
            regularUser = userRepository.save(regularUser);
            System.out.println("Created regular user with ID: " + regularUser.getId());

            // Generate JWT tokens
            System.out.println("Generating JWT tokens...");
            adminToken = jwtUtils.generateTokenFromUsernameAndRoles(
                    adminUser.getEmail(), 
                    List.of("ROLE_ADMIN")
            );
            
            userToken = jwtUtils.generateTokenFromUsernameAndRoles(
                    regularUser.getEmail(), 
                    List.of("ROLE_USER")
            );
            
            System.out.println("Admin token: " + (adminToken != null ? "Generated" : "NULL"));
            System.out.println("User token: " + (userToken != null ? "Generated" : "NULL"));
            System.out.println("=== Test Setup Completed ===");
        } catch (Exception e) {
            System.out.println("Error in user/token setup: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    // Test creating a role with admin privileges
    @Test
    void createRole_WithAdminToken_Success() throws Exception {
        RoleRequest roleRequest = new RoleRequest();
        roleRequest.setName("MANAGER");
        roleRequest.setDescription("Manager role");

        mockMvc.perform(post("/api/roles")
                .with(csrf())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(roleRequest)))
                .andDo(result -> {
                    System.out.println("Response Status: " + result.getResponse().getStatus());
                    System.out.println("Response Body: " + result.getResponse().getContentAsString());
                    if (result.getResolvedException() != null) {
                        System.out.println("Exception: " + result.getResolvedException().getMessage());
                        result.getResolvedException().printStackTrace();
                    }
                })
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("MANAGER"))
                .andExpect(jsonPath("$.data.description").value("Manager role"))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    // Test creating a role without authentication
    @Test
    void createRole_WithoutToken_Unauthorized() throws Exception {
        RoleRequest roleRequest = new RoleRequest();
        roleRequest.setName("MANAGER");
        roleRequest.setDescription("Manager role");

        mockMvc.perform(post("/api/roles")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(roleRequest)))
                .andExpect(status().isUnauthorized());
    }

    // Test creating a role with non-admin token
    @Test
    void createRole_WithUserToken_Forbidden() throws Exception {
        RoleRequest roleRequest = new RoleRequest();
        roleRequest.setName("MANAGER");
        roleRequest.setDescription("Manager role");

        mockMvc.perform(post("/api/roles")
                .with(csrf())
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(roleRequest)))
                .andExpect(status().isForbidden());
    }

    // Test creating a role with invalid token
    @Test
    void createRole_WithInvalidToken_Unauthorized() throws Exception {
        RoleRequest roleRequest = new RoleRequest();
        roleRequest.setName("MANAGER");
        roleRequest.setDescription("Manager role");

        mockMvc.perform(post("/api/roles")
                .with(csrf())
                .header("Authorization", "Bearer invalid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(roleRequest)))
                .andExpect(status().isUnauthorized());
    }

    // Test getting all roles with admin privileges
    @Test
    void getAllRoles_WithAdminToken_Success() throws Exception {
        mockMvc.perform(get("/api/roles")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    // Test getting all roles without authentication
    @Test
    void getAllRoles_WithoutToken_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/roles"))
                .andExpect(status().isUnauthorized());
    }

    // Test getting all roles with non-admin token
    @Test
    void getAllRoles_WithUserToken_Forbidden() throws Exception {
        mockMvc.perform(get("/api/roles")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    // Test assigning roles to user with admin privileges
    @Test
    void assignRolesToUser_WithAdminToken_Success() throws Exception {
        System.out.println("=== assignRolesToUser_WithAdminToken_Success ===");
        System.out.println("Admin Role ID: " + adminRole.getId());
        System.out.println("Regular User ID: " + regularUser.getId());
        System.out.println("Admin Token: " + (adminToken != null ? "Present" : "NULL"));
        
        AssignRoleRequest assignRoleRequest = new AssignRoleRequest();
        assignRoleRequest.setRoleIds(Set.of(adminRole.getId()));
        
        System.out.println("Request body: " + objectMapper.writeValueAsString(assignRoleRequest));

        mockMvc.perform(post("/api/roles/users/{userId}/roles", regularUser.getId())
                .with(csrf())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(assignRoleRequest)))
                .andDo(result -> {
                    System.out.println("Response Status: " + result.getResponse().getStatus());
                    System.out.println("Response Body: " + result.getResponse().getContentAsString());
                    if (result.getResolvedException() != null) {
                        System.out.println("Exception: " + result.getResolvedException().getMessage());
                        result.getResolvedException().printStackTrace();
                    }
                })
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(regularUser.getId()))
                .andExpect(jsonPath("$.data.roles").isArray())
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    // Test assigning roles to user without authentication
    @Test
    void assignRolesToUser_WithoutToken_Unauthorized() throws Exception {
        AssignRoleRequest assignRoleRequest = new AssignRoleRequest();
        assignRoleRequest.setRoleIds(Set.of(adminRole.getId()));

        mockMvc.perform(post("/api/roles/users/{userId}/roles", regularUser.getId())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(assignRoleRequest)))
                .andExpect(status().isUnauthorized());
    }

    // Test assigning roles to user with non-admin token
    @Test
    void assignRolesToUser_WithUserToken_Forbidden() throws Exception {
        AssignRoleRequest assignRoleRequest = new AssignRoleRequest();
        assignRoleRequest.setRoleIds(Set.of(adminRole.getId()));

        mockMvc.perform(post("/api/roles/users/{userId}/roles", regularUser.getId())
                .with(csrf())
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(assignRoleRequest)))
                .andExpect(status().isForbidden());
    }

    // Test assigning roles to non-existent user
    @Test
    void assignRolesToUser_NonExistentUser_NotFound() throws Exception {
        System.out.println("=== assignRolesToUser_NonExistentUser_NotFound ===");
        System.out.println("Admin Role ID: " + adminRole.getId());
        System.out.println("Using non-existent user ID: 99999");
        
        AssignRoleRequest assignRoleRequest = new AssignRoleRequest();
        assignRoleRequest.setRoleIds(Set.of(adminRole.getId()));
        
        System.out.println("Request body: " + objectMapper.writeValueAsString(assignRoleRequest));

        mockMvc.perform(post("/api/roles/users/{userId}/roles", 99999L)
                .with(csrf())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(assignRoleRequest)))
                .andDo(result -> {
                    System.out.println("Response Status: " + result.getResponse().getStatus());
                    System.out.println("Response Body: " + result.getResponse().getContentAsString());
                    if (result.getResolvedException() != null) {
                        System.out.println("Exception: " + result.getResolvedException().getMessage());
                        result.getResolvedException().printStackTrace();
                    }
                })
                .andExpect(status().isNotFound());
    }

    // Test creating role with invalid data
    @Test
    void createRole_WithInvalidData_BadRequest() throws Exception {
        RoleRequest roleRequest = new RoleRequest();
        // Missing required name field

        mockMvc.perform(post("/api/roles")
                .with(csrf())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(roleRequest)))
                .andExpect(status().isBadRequest());
    }

    // Test creating duplicate role
    @Test
    void createRole_DuplicateName_Conflict() throws Exception {
        RoleRequest roleRequest = new RoleRequest();
        roleRequest.setName("ADMIN"); // Already exists
        roleRequest.setDescription("Duplicate admin role");

        mockMvc.perform(post("/api/roles")
                .with(csrf())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(roleRequest)))
                .andExpect(status().isConflict());
    }
}
