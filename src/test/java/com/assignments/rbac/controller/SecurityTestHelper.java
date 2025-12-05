package com.assignments.rbac.controller;

import com.assignments.rbac.entity.Role;
import com.assignments.rbac.entity.User;
import com.assignments.rbac.repository.RoleRepository;
import com.assignments.rbac.repository.UserRepository;
import com.assignments.rbac.security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * Helper class for setting up test data and JWT tokens for integration tests.
 * This class provides common functionality for creating users, roles, and tokens
 * across different test classes.
 */
@Component
public class SecurityTestHelper {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    /**
     * Creates a test role with the given name and description.
     */
    public Role createRole(String name, String description) {
        Role role = new Role();
        role.setName(name);
        role.setDescription(description);
        return roleRepository.save(role);
    }

    /**
     * Creates a test user with the given details and roles.
     */
    public User createUser(String name, String username, String email, String password, Set<Role> roles) {
        User user = new User();
        user.setName(name);
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRoles(roles);
        return userRepository.save(user);
    }

    /**
     * Creates an admin user with ADMIN role.
     */
    public User createAdminUser(String name, String username, String email, String password) {
        Role adminRole = createRole("ADMIN", "Administrator role");
        return createUser(name, username, email, password, Set.of(adminRole));
    }

    /**
     * Creates a regular user with USER role.
     */
    public User createRegularUser(String name, String username, String email, String password) {
        Role userRole = createRole("USER", "Regular user role");
        return createUser(name, username, email, password, Set.of(userRole));
    }

    /**
     * Generates a JWT token for the given user with their roles.
     */
    public String generateTokenForUser(User user) {
        List<String> roleNames = user.getRoles().stream()
                .map(role -> "ROLE_" + role.getName())
                .toList();
        return jwtUtils.generateTokenFromUsernameAndRoles(user.getEmail(), roleNames);
    }

    /**
     * Generates a JWT token with specific roles.
     */
    public String generateTokenWithRoles(String email, List<String> roles) {
        return jwtUtils.generateTokenFromUsernameAndRoles(email, roles);
    }

    /**
     * Creates a complete test setup with admin and regular users, returning both users and their tokens.
     */
    public TestUserSetup createCompleteTestSetup() {
        // Clean up existing data
        userRepository.deleteAll();
        roleRepository.deleteAll();

        // Create users
        User adminUser = createAdminUser("Admin User", "admin", "admin@test.com", "password123");
        User regularUser = createRegularUser("Regular User", "user", "user@test.com", "password123");

        // Generate tokens
        String adminToken = generateTokenForUser(adminUser);
        String userToken = generateTokenForUser(regularUser);

        return new TestUserSetup(adminUser, regularUser, adminToken, userToken);
    }

    /**
     * Creates test roles commonly used in tests.
     */
    public RoleSetup createStandardRoles() {
        Role adminRole = createRole("ADMIN", "Administrator role");
        Role userRole = createRole("USER", "Regular user role");
        Role managerRole = createRole("MANAGER", "Manager role");
        
        return new RoleSetup(adminRole, userRole, managerRole);
    }

    /**
     * Cleans up all test data from repositories.
     */
    public void cleanupTestData() {
        userRepository.deleteAll();
        roleRepository.deleteAll();
    }

    /**
     * Data class to hold test user setup information.
     */
    public static class TestUserSetup {
        private final User adminUser;
        private final User regularUser;
        private final String adminToken;
        private final String userToken;

        public TestUserSetup(User adminUser, User regularUser, String adminToken, String userToken) {
            this.adminUser = adminUser;
            this.regularUser = regularUser;
            this.adminToken = adminToken;
            this.userToken = userToken;
        }

        public User getAdminUser() { return adminUser; }
        public User getRegularUser() { return regularUser; }
        public String getAdminToken() { return adminToken; }
        public String getUserToken() { return userToken; }
    }

    /**
     * Data class to hold standard role setup information.
     */
    public static class RoleSetup {
        private final Role adminRole;
        private final Role userRole;
        private final Role managerRole;

        public RoleSetup(Role adminRole, Role userRole, Role managerRole) {
            this.adminRole = adminRole;
            this.userRole = userRole;
            this.managerRole = managerRole;
        }

        public Role getAdminRole() { return adminRole; }
        public Role getUserRole() { return userRole; }
        public Role getManagerRole() { return managerRole; }
    }
}
