package com.assignments.rbac.service;

import com.assignments.rbac.entity.User;
import com.assignments.rbac.repository.UserRepository;
import com.assignments.rbac.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("Loading user by email: {}", email);
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        
        log.debug("User found: ID={}, Email={}", user.getId(), user.getEmail());
        
        // Fetch roles separately
        user.setRoles(new java.util.HashSet<>(roleRepository.findRolesByUserId(user.getId())));
        log.debug("Roles fetched separately. Size: {}", user.getRoles().size());

        try {
            log.debug("Building UserDetails with authorities...");
            return org.springframework.security.core.userdetails.User.builder()
                    .username(user.getEmail())
                    .password(user.getPassword())
                    .authorities(user.getRoles().stream()
                            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                            .collect(Collectors.toList()))
                    .build();
        } catch (Exception e) {
            log.error("Error building UserDetails: {}", e.getMessage(), e);
            throw e;
        }
    }
}
