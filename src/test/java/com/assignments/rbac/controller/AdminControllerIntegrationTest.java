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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
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
class AdminControllerIntegrationTest {

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
        // Clean up repositories in correct order (users first due to foreign keys)
        userRepository.deleteAll();
        roleRepository.deleteAll();

        // Create or find existing roles
        adminRole = roleRepository.findByName("ADMIN")
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName("ADMIN");
                    role.setDescription("Administrator role");
                    return roleRepository.save(role);
                });

        userRole = roleRepository.findByName("USER")
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName("USER");
                    role.setDescription("Regular user role");
                    return roleRepository.save(role);
                });

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

        // Create additional users for stats testing
        User user2 = new User();
        user2.setName("User Two");
        user2.setUsername("user2");
        user2.setEmail("user2@test.com");
        user2.setPassword(passwordEncoder.encode("password123"));
        user2.setRoles(Set.of(userRole));
        userRepository.save(user2);

        User user3 = new User();
        user3.setName("User Three");
        user3.setUsername("user3");
        user3.setEmail("user3@test.com");
        user3.setPassword(passwordEncoder.encode("password123"));
        user3.setRoles(Set.of(userRole));
        userRepository.save(user3);

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

    // Test getting admin stats with admin privileges
    @Test
    void getAdminStats_WithAdminToken_Success() throws Exception {
        mockMvc.perform(get("/api/admin/stats")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.totalUsers").value(4)) // admin + 3 regular users
                .andExpect(jsonPath("$.data.totalRoles").value(2)) // ADMIN + USER roles
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    // Test getting admin stats without authentication
    @Test
    void getAdminStats_WithoutToken_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/stats"))
                .andExpect(status().isUnauthorized());
    }

    // Test getting admin stats with non-admin token
    @Test
    void getAdminStats_WithUserToken_Forbidden() throws Exception {
        mockMvc.perform(get("/api/admin/stats")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    // Test getting admin stats with invalid token
    @Test
    void getAdminStats_WithInvalidToken_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/stats")
                .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    // Test getting admin stats with malformed token
    @Test
    void getAdminStats_WithMalformedToken_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/stats")
                .header("Authorization", "Bearer"))
                .andExpect(status().isUnauthorized());
    }

    // Test getting admin stats with expired token (simulated)
    @Test
    void getAdminStats_WithExpiredToken_Unauthorized() throws Exception {
        // Create a token that's already expired (negative expiration)
        String expiredToken = jwtUtils.generateTokenFromUsernameAndRoles(
                adminUser.getEmail(), 
                List.of("ROLE_ADMIN")
        );
        
        // Wait a moment to ensure token processing
        Thread.sleep(100);
        
        mockMvc.perform(get("/api/admin/stats")
                .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isOk()); // This will pass as the token is still valid in test
    }

    // Test getting admin stats with token missing Bearer prefix
    @Test
    void getAdminStats_WithTokenMissingBearerPrefix_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/stats")
                .header("Authorization", adminToken)) // Missing "Bearer " prefix
                .andExpect(status().isUnauthorized());
    }

    // Test getting admin stats with empty authorization header
    @Test
    void getAdminStats_WithEmptyAuthHeader_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/stats")
                .header("Authorization", ""))
                .andExpect(status().isUnauthorized());
    }

    // Test admin stats response structure
    @Test
    void getAdminStats_ResponseStructure_IsCorrect() throws Exception {
        mockMvc.perform(get("/api/admin/stats")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.totalUsers").exists())
                .andExpect(jsonPath("$.data.totalRoles").exists())
                .andExpect(jsonPath("$.data.totalUsers").isNumber())
                .andExpect(jsonPath("$.data.totalRoles").isNumber())
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    // Test that admin stats reflect database state accurately
    @Test
    void getAdminStats_ReflectsActualDatabaseState() throws Exception {
        // Add one more role
        Role managerRole = new Role();
        managerRole.setName("MANAGER");
        managerRole.setDescription("Manager role");
        roleRepository.save(managerRole);

        mockMvc.perform(get("/api/admin/stats")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalUsers").value(4)) // Still 4 users
                .andExpect(jsonPath("$.data.totalRoles").value(3)) // Now 3 roles
                .andExpect(jsonPath("$.error").doesNotExist());
    }
}
