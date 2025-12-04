package com.assignments.rbac.service;

import com.assignments.rbac.dto.AssignRoleRequest;
import com.assignments.rbac.dto.RoleRequest;
import com.assignments.rbac.dto.RoleResponse;
import com.assignments.rbac.dto.UserResponse;
import com.assignments.rbac.entity.Role;
import com.assignments.rbac.entity.User;
import com.assignments.rbac.exception.UserAlreadyExistsException;
import com.assignments.rbac.exception.UserNotFoundException;
import com.assignments.rbac.mapper.RoleMapper;
import com.assignments.rbac.mapper.UserMapper;
import com.assignments.rbac.repository.RoleRepository;
import com.assignments.rbac.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleMapper roleMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private RoleService roleService;

    private RoleRequest roleRequest;
    private Role role;
    private RoleResponse roleResponse;
    private User user;
    private UserResponse userResponse;
    private AssignRoleRequest assignRoleRequest;

    @BeforeEach
    void setUp() {
        roleRequest = new RoleRequest();
        roleRequest.setName("MANAGER");
        roleRequest.setDescription("Manager role");

        role = new Role();
        role.setId(1L);
        role.setName("MANAGER");
        role.setDescription("Manager role");

        roleResponse = new RoleResponse();
        roleResponse.setId(1L);
        roleResponse.setName("MANAGER");
        roleResponse.setDescription("Manager role");

        user = new User();
        user.setId(1L);
        user.setName("Harsh");
        user.setEmail("harsh@test.com");
        user.setRoles(new HashSet<>());

        userResponse = new UserResponse();
        userResponse.setId(1L);
        userResponse.setName("Harsh");
        userResponse.setEmail("harsh@test.com");

        assignRoleRequest = new AssignRoleRequest();
        assignRoleRequest.setRoleIds(Set.of(1L));
    }

    @Test
    void createRole_Success() {
        when(roleRepository.existsByName(anyString())).thenReturn(false);
        when(roleMapper.toEntity(any(RoleRequest.class))).thenReturn(role);
        when(roleRepository.save(any(Role.class))).thenReturn(role);
        when(roleMapper.toResponse(any(Role.class))).thenReturn(roleResponse);

        RoleResponse result = roleService.createRole(roleRequest);

        assertNotNull(result);
        assertEquals("MANAGER", result.getName());
        assertEquals("Manager role", result.getDescription());

        verify(roleRepository).existsByName("MANAGER");
        verify(roleRepository).save(any(Role.class));
    }

    @Test
    void createRole_RoleAlreadyExists() {
        when(roleRepository.existsByName(anyString())).thenReturn(true);

        UserAlreadyExistsException exception = assertThrows(UserAlreadyExistsException.class,
                () -> roleService.createRole(roleRequest));

        assertEquals("Role with name 'MANAGER' already exists", exception.getMessage());
        verify(roleRepository).existsByName("MANAGER");
        verify(roleRepository, never()).save(any(Role.class));
    }

    @Test
    void assignRolesToUser_Success() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(user));
        when(roleRepository.findByIdIn(any())).thenReturn(Set.of(role));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toResponse(any(User.class))).thenReturn(userResponse);

        UserResponse result = roleService.assignRolesToUser(1L, assignRoleRequest);

        assertNotNull(result);
        assertEquals("Harsh", result.getName());

        verify(userRepository).findById(1L);
        verify(roleRepository).findByIdIn(Set.of(1L));
        verify(userRepository).save(any(User.class));
    }

    @Test
    void assignRolesToUser_UserNotFound() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(UserNotFoundException.class,
                () -> roleService.assignRolesToUser(1L, assignRoleRequest));

        assertEquals("User not found with id: 1", exception.getMessage());
        verify(userRepository).findById(1L);
        verify(roleRepository, never()).findByIdIn(any());
    }

    @Test
    void assignRolesToUser_RoleNotFound() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(user));
        when(roleRepository.findByIdIn(any())).thenReturn(new HashSet<>());

        UserNotFoundException exception = assertThrows(UserNotFoundException.class,
                () -> roleService.assignRolesToUser(1L, assignRoleRequest));

        assertEquals("One or more roles not found", exception.getMessage());
        verify(userRepository).findById(1L);
        verify(roleRepository).findByIdIn(Set.of(1L));
    }
}

