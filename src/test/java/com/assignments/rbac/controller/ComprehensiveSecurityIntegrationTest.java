package com.assignments.rbac.controller;

import com.assignments.rbac.dto.AssignRoleRequest;
import com.assignments.rbac.dto.RoleRequest;
import com.assignments.rbac.entity.Role;
import com.assignments.rbac.entity.User;
import com.assignments.rbac.repository.RoleRepository;
import com.assignments.rbac.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Set;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive integration test that tests security across all controllers
 * and verifies end-to-end security workflows.
 */
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
class ComprehensiveSecurityIntegrationTest {

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
    private SecurityTestHelper securityTestHelper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private SecurityTestHelper.TestUserSetup testSetup;

    @BeforeEach
    void setUp() {
        testSetup = securityTestHelper.createCompleteTestSetup();
    }

    /**
     * Test complete workflow: Admin creates role, assigns it to user, user can access their profile
     */
    @Test
    void completeWorkflow_AdminCreatesRoleAssignsToUser_UserCanAccessProfile() throws Exception {
        // Step 1: Admin creates a new role
        RoleRequest roleRequest = new RoleRequest();
        roleRequest.setName("MANAGER");
        roleRequest.setDescription("Manager role");

        String roleResponse = mockMvc.perform(post("/api/roles")
                .with(csrf())
                .header("Authorization", "Bearer " + testSetup.getAdminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(roleRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("MANAGER"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Step 2: Admin assigns the new role to regular user
        Role managerRole = roleRepository.findByName("MANAGER").orElseThrow();
        AssignRoleRequest assignRoleRequest = new AssignRoleRequest();
        assignRoleRequest.setRoleIds(Set.of(managerRole.getId()));

        mockMvc.perform(post("/api/roles/users/{userId}/roles", testSetup.getRegularUser().getId())
                .with(csrf())
                .header("Authorization", "Bearer " + testSetup.getAdminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(assignRoleRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Step 3: User can still access their profile (role assignment doesn't affect existing token)
        mockMvc.perform(get("/api/users/me")
                .header("Authorization", "Bearer " + testSetup.getUserToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("user@test.com"));

        // Step 4: Admin can view stats reflecting the changes
        mockMvc.perform(get("/api/admin/stats")
                .header("Authorization", "Bearer " + testSetup.getAdminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalRoles").value(3)); // ADMIN, USER, MANAGER
    }

    /**
     * Test that all secured endpoints reject requests without authentication
     */
    @Test
    void allSecuredEndpoints_WithoutAuthentication_ReturnUnauthorized() throws Exception {
        // Role endpoints
        mockMvc.perform(get("/api/roles"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/roles")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/roles/users/1/roles")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized());

        // Admin endpoints
        mockMvc.perform(get("/api/admin/stats"))
                .andExpect(status().isUnauthorized());

        // Auth secured endpoints
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Test that regular user cannot access admin-only endpoints
     */
    @Test
    void regularUser_CannotAccessAdminEndpoints() throws Exception {
        String userToken = testSetup.getUserToken();

        // Role management endpoints (admin only)
        mockMvc.perform(get("/api/roles")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/roles")
                .with(csrf())
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"TEST\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/roles/users/1/roles")
                .with(csrf())
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"roleIds\":[1]}"))
                .andExpect(status().isForbidden());

        // Admin stats endpoint
        mockMvc.perform(get("/api/admin/stats")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    /**
     * Test that admin can access all endpoints
     */
    @Test
    void admin_CanAccessAllEndpoints() throws Exception {
        String adminToken = testSetup.getAdminToken();

        // Role management endpoints
        mockMvc.perform(get("/api/roles")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // Admin stats endpoint
        mockMvc.perform(get("/api/admin/stats")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // User profile endpoint
        mockMvc.perform(get("/api/users/me")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    /**
     * Test security with malformed or invalid tokens
     */
    @Test
    void securedEndpoints_WithInvalidTokens_ReturnUnauthorized() throws Exception {
        String[] invalidTokens = {
                "invalid-token",
                "Bearer",
                "",
                "Bearer ",
                "Bearer invalid-jwt-token",
                "Basic dGVzdDp0ZXN0" // Basic auth instead of Bearer
        };

        for (String invalidToken : invalidTokens) {
            // Test role endpoint
            mockMvc.perform(get("/api/roles")
                    .header("Authorization", invalidToken))
                    .andExpect(status().isUnauthorized());

            // Test admin endpoint
            mockMvc.perform(get("/api/admin/stats")
                    .header("Authorization", invalidToken))
                    .andExpect(status().isUnauthorized());

            // Test user profile endpoint
            mockMvc.perform(get("/api/users/me")
                    .header("Authorization", invalidToken))
                    .andExpect(status().isUnauthorized());
        }
    }

    /**
     * Test CSRF protection on state-changing operations
     */
    @Test
    void stateChangingOperations_WithoutCSRF_ReturnForbidden() throws Exception {
        String adminToken = testSetup.getAdminToken();

        // POST operations without CSRF should fail
        mockMvc.perform(post("/api/roles")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"TEST\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/roles/users/1/roles")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"roleIds\":[1]}"))
                .andExpect(status().isForbidden());
    }

    /**
     * Test that users can only access their own profile data
     */
    @Test
    void userProfile_ReturnsCorrectUserData() throws Exception {
        // Admin accessing their profile
        mockMvc.perform(get("/api/users/me")
                .header("Authorization", "Bearer " + testSetup.getAdminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("admin@test.com"))
                .andExpect(jsonPath("$.data.username").value("admin"));

        // Regular user accessing their profile
        mockMvc.perform(get("/api/users/me")
                .header("Authorization", "Bearer " + testSetup.getUserToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("user@test.com"))
                .andExpect(jsonPath("$.data.username").value("user"));
    }

    /**
     * Test role hierarchy and permissions
     */
    @Test
    void roleHierarchy_AdminCanManageAllRoles_UserCannot() throws Exception {
        // Create additional role as admin
        RoleRequest roleRequest = new RoleRequest();
        roleRequest.setName("SUPERVISOR");
        roleRequest.setDescription("Supervisor role");

        mockMvc.perform(post("/api/roles")
                .with(csrf())
                .header("Authorization", "Bearer " + testSetup.getAdminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(roleRequest)))
                .andExpect(status().isCreated());

        // Admin can view all roles
        mockMvc.perform(get("/api/roles")
                .header("Authorization", "Bearer " + testSetup.getAdminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3)); // ADMIN, USER, SUPERVISOR

        // Regular user cannot view roles
        mockMvc.perform(get("/api/roles")
                .header("Authorization", "Bearer " + testSetup.getUserToken()))
                .andExpect(status().isForbidden());
    }

    /**
     * Test concurrent access and token validation
     */
    @Test
    void concurrentAccess_MultipleValidTokens_AllSucceed() throws Exception {
        // Both admin and user should be able to access their respective allowed endpoints simultaneously
        
        // Admin accesses admin endpoint
        mockMvc.perform(get("/api/admin/stats")
                .header("Authorization", "Bearer " + testSetup.getAdminToken()))
                .andExpect(status().isOk());

        // User accesses profile endpoint
        mockMvc.perform(get("/api/users/me")
                .header("Authorization", "Bearer " + testSetup.getUserToken()))
                .andExpect(status().isOk());

        // Admin can also access profile endpoint
        mockMvc.perform(get("/api/users/me")
                .header("Authorization", "Bearer " + testSetup.getAdminToken()))
                .andExpect(status().isOk());
    }

    /**
     * Test that security configuration properly handles OPTIONS requests (CORS preflight)
     */
    @Test
    void optionsRequests_AreHandledCorrectly() throws Exception {
        // OPTIONS requests should not require authentication for CORS preflight
        mockMvc.perform(options("/api/roles"))
                .andExpect(status().isOk());

        mockMvc.perform(options("/api/admin/stats"))
                .andExpect(status().isOk());

        mockMvc.perform(options("/api/users/me"))
                .andExpect(status().isOk());
    }
}

