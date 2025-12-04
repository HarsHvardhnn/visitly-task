package com.assignments.rbac.repository;

import com.assignments.rbac.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setName("Harsh");
        testUser.setUsername("harsh");
        testUser.setEmail("harsh@test.com");
        testUser.setPassword("encodedPassword");
    }

    @Test
    void findByUsername_UserExists() {
        entityManager.persistAndFlush(testUser);

        Optional<User> found = userRepository.findByUsername("harsh");

        assertTrue(found.isPresent());
        assertEquals("harsh", found.get().getUsername());
        assertEquals("Harsh", found.get().getName());
        assertEquals("harsh@test.com", found.get().getEmail());
    }

    @Test
    void findByUsername_UserNotExists() {
        Optional<User> found = userRepository.findByUsername("nonexistent");

        assertFalse(found.isPresent());
    }

    @Test
    void findByEmail_UserExists() {
        entityManager.persistAndFlush(testUser);

        Optional<User> found = userRepository.findByEmail("harsh@test.com");

        assertTrue(found.isPresent());
        assertEquals("harsh@test.com", found.get().getEmail());
        assertEquals("harsh", found.get().getUsername());
    }

    @Test
    void findByEmail_UserNotExists() {
        Optional<User> found = userRepository.findByEmail("nonexistent@example.com");

        assertFalse(found.isPresent());
    }

    @Test
    void existsByUsername_UserExists() {
        entityManager.persistAndFlush(testUser);

        boolean exists = userRepository.existsByUsername("harsh");

        assertTrue(exists);
    }

    @Test
    void existsByUsername_UserNotExists() {
        boolean exists = userRepository.existsByUsername("nonexistent");

        assertFalse(exists);
    }

    @Test
    void existsByEmail_UserExists() {
        entityManager.persistAndFlush(testUser);

        boolean exists = userRepository.existsByEmail("harsh@test.com");

        assertTrue(exists);
    }

    @Test
    void existsByEmail_UserNotExists() {
        boolean exists = userRepository.existsByEmail("nonexistent@example.com");

        assertFalse(exists);
    }

    @Test
    void save_UserWithAllFields() {
        User savedUser = userRepository.save(testUser);

        assertNotNull(savedUser.getId());
        assertEquals("Harsh", savedUser.getName());
        assertEquals("harsh", savedUser.getUsername());
        assertEquals("harsh@test.com", savedUser.getEmail());
        assertEquals("encodedPassword", savedUser.getPassword());
        assertNotNull(savedUser.getCreatedAt());
    }

    @Test
    void save_DuplicateUsername_ThrowsException() {
        entityManager.persistAndFlush(testUser);

        User duplicateUser = new User();
        duplicateUser.setName("Test User");
        duplicateUser.setUsername("harsh");
        duplicateUser.setEmail("test@example.com");
        duplicateUser.setPassword("encodedPassword");

        assertThrows(Exception.class, () -> {
            userRepository.saveAndFlush(duplicateUser);
        });
    }

    @Test
    void save_DuplicateEmail_ThrowsException() {
        entityManager.persistAndFlush(testUser);

        User duplicateUser = new User();
        duplicateUser.setName("Test User");
        duplicateUser.setUsername("testuser");
        duplicateUser.setEmail("harsh@test.com");
        duplicateUser.setPassword("encodedPassword");

        assertThrows(Exception.class, () -> {
            userRepository.saveAndFlush(duplicateUser);
        });
    }
}
