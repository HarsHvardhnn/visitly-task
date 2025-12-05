package com.assignments.rbac.controller;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = com.assignments.rbac.RbacApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, statements = {
        "SET FOREIGN_KEY_CHECKS = 0",
        "TRUNCATE TABLE user_roles",
        "TRUNCATE TABLE users", 
        "TRUNCATE TABLE roles",
        "SET FOREIGN_KEY_CHECKS = 1"
})
class AuthControllerSecuredEndpointsTest {

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
        System.out.println("=== AuthControllerSecuredEndpointsTest setUp() starting ===");
        
        // Create roles defensively to avoid duplicate key errors
        adminRole = roleRepository.findByName("ADMIN").orElseGet(() -> {
            Role role = new Role();
            role.setName("ADMIN");
            role.setDescription("Administrator role");
            return roleRepository.save(role);
        });
        System.out.println("Admin role created/found: " + adminRole.getId());

        userRole = roleRepository.findByName("USER").orElseGet(() -> {
            Role role = new Role();
            role.setName("USER");
            role.setDescription("Regular user role");
            return roleRepository.save(role);
        });
        System.out.println("User role created/found: " + userRole.getId());

        // Create admin user
        adminUser = new User();
        adminUser.setName("Admin User");
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@test.com");
        adminUser.setPassword(passwordEncoder.encode("password123"));
        adminUser.setRoles(Set.of(adminRole));
        adminUser = userRepository.save(adminUser);

        // Create regular user
        regularUser = new User();
        regularUser.setName("Regular User");
        regularUser.setUsername("user");
        regularUser.setEmail("user@test.com");
        regularUser.setPassword(passwordEncoder.encode("password123"));
        regularUser.setRoles(Set.of(userRole));
        regularUser = userRepository.save(regularUser);

        // Generate JWT tokens
        adminToken = jwtUtils.generateTokenFromUsernameAndRoles(
                adminUser.getEmail(), 
                List.of("ROLE_ADMIN")
        );
        
        userToken = jwtUtils.generateTokenFromUsernameAndRoles(
                regularUser.getEmail(), 
                List.of("ROLE_USER")
        );
    }

    // Test getting current user info with valid admin token
    @Test
    void getCurrentUser_WithAdminToken_Success() throws Exception {
        mockMvc.perform(get("/api/users/me")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.id").value(adminUser.getId()))
                .andExpect(jsonPath("$.data.name").value("Admin User"))
                .andExpect(jsonPath("$.data.username").value("admin"))
                .andExpect(jsonPath("$.data.email").value("admin@test.com"))
                .andExpect(jsonPath("$.data.roles").isArray())
                .andExpect(jsonPath("$.data.roles[0].name").value("ADMIN"))
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    // Test getting current user info with valid regular user token
    @Test
    void getCurrentUser_WithUserToken_Success() throws Exception {
        mockMvc.perform(get("/api/users/me")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.id").value(regularUser.getId()))
                .andExpect(jsonPath("$.data.name").value("Regular User"))
                .andExpect(jsonPath("$.data.username").value("user"))
                .andExpect(jsonPath("$.data.email").value("user@test.com"))
                .andExpect(jsonPath("$.data.roles").isArray())
                .andExpect(jsonPath("$.data.roles[0].name").value("USER"))
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    // Test getting current user info without authentication
    @Test
    void getCurrentUser_WithoutToken_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    // Test getting current user info with invalid token
    @Test
    void getCurrentUser_WithInvalidToken_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/users/me")
                .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    // Test getting current user info with malformed token
    @Test
    void getCurrentUser_WithMalformedToken_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/users/me")
                .header("Authorization", "Bearer"))
                .andExpect(status().isUnauthorized());
    }

    // Test getting current user info with token missing Bearer prefix
    @Test
    void getCurrentUser_WithTokenMissingBearerPrefix_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/users/me")
                .header("Authorization", userToken)) // Missing "Bearer " prefix
                .andExpect(status().isUnauthorized());
    }

    // Test getting current user info with empty authorization header
    @Test
    void getCurrentUser_WithEmptyAuthHeader_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/users/me")
                .header("Authorization", ""))
                .andExpect(status().isUnauthorized());
    }

    // Test that current user endpoint returns correct user data structure
    @Test
    void getCurrentUser_ResponseStructure_IsCorrect() throws Exception {
        mockMvc.perform(get("/api/users/me")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.name").exists())
                .andExpect(jsonPath("$.data.username").exists())
                .andExpect(jsonPath("$.data.email").exists())
                .andExpect(jsonPath("$.data.roles").isArray())
                .andExpect(jsonPath("$.data.createdAt").exists())
                .andExpect(jsonPath("$.data.lastUpdatedAt").exists())
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    // Test that current user endpoint doesn't expose password
    @Test
    void getCurrentUser_DoesNotExposePassword() throws Exception {
        mockMvc.perform(get("/api/users/me")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    // Test getting current user info with user having multiple roles
    @Test
    void getCurrentUser_WithMultipleRoles_Success() throws Exception {
        // Create a user with multiple roles
        Role managerRole = new Role();
        managerRole.setName("MANAGER");
        managerRole.setDescription("Manager role");
        managerRole = roleRepository.save(managerRole);

        User multiRoleUser = new User();
        multiRoleUser.setName("Multi Role User");
        multiRoleUser.setUsername("multirole");
        multiRoleUser.setEmail("multirole@test.com");
        multiRoleUser.setPassword(passwordEncoder.encode("password123"));
        multiRoleUser.setRoles(Set.of(userRole, managerRole));
        multiRoleUser = userRepository.save(multiRoleUser);

        String multiRoleToken = jwtUtils.generateTokenFromUsernameAndRoles(
                multiRoleUser.getEmail(), 
                List.of("ROLE_USER", "ROLE_MANAGER")
        );

        mockMvc.perform(get("/api/users/me")
                .header("Authorization", "Bearer " + multiRoleToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(multiRoleUser.getId()))
                .andExpect(jsonPath("$.data.roles").isArray())
                .andExpect(jsonPath("$.data.roles.length()").value(2))
                .andExpect(jsonPath("$.error").doesNotExist());
    }


    // Test current user endpoint with case-insensitive email lookup
    @Test
    void getCurrentUser_WithDifferentCaseEmail_Success() throws Exception {
        // Create token with uppercase email
        String upperCaseEmailToken = jwtUtils.generateTokenFromUsernameAndRoles(
                regularUser.getEmail().toUpperCase(), 
                List.of("ROLE_USER")
        );

        // Email lookup should be case-insensitive, so this should succeed
        mockMvc.perform(get("/api/users/me")
                .header("Authorization", "Bearer " + upperCaseEmailToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value(regularUser.getEmail())); // Should return lowercase email from DB
    }
}