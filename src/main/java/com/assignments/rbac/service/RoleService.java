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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class RoleService {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final RoleMapper roleMapper;
    private final UserMapper userMapper;

    public RoleResponse createRole(RoleRequest request) {
        if (roleRepository.existsByName(request.getName())) {
            throw new UserAlreadyExistsException("Role with name '" + request.getName() + "' already exists");
        }

        Role role = roleMapper.toEntity(request);
        Role savedRole = roleRepository.save(role);
        return roleMapper.toResponse(savedRole);
    }

    public UserResponse assignRolesToUser(Long userId, AssignRoleRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        Set<Role> roles = roleRepository.findByIdIn(request.getRoleIds());
        
        if (roles.size() != request.getRoleIds().size()) {
            throw new UserNotFoundException("One or more roles not found");
        }

        Set<Role> userRoles = new HashSet<>(user.getRoles());
        userRoles.addAll(roles);
        user.setRoles(userRoles);
        User savedUser = userRepository.save(user);
        return userMapper.toResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public Set<Role> findRolesByIds(Set<Long> roleIds) {
        return roleRepository.findByIdIn(roleIds);
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> getAllRoles() {
        List<Role> roles = roleRepository.findAll();
        return roles.stream()
                .map(roleMapper::toResponse)
                .toList();
    }
}

