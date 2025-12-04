package com.assignments.rbac.service;

import com.assignments.rbac.dto.CurrentUserResponse;
import com.assignments.rbac.dto.LoginRequest;
import com.assignments.rbac.dto.LoginResponse;
import com.assignments.rbac.dto.UserRegistrationRequest;
import com.assignments.rbac.dto.UserResponse;
import com.assignments.rbac.entity.User;
import com.assignments.rbac.exception.UserAlreadyExistsException;
import com.assignments.rbac.exception.UserNotFoundException;
import com.assignments.rbac.mapper.UserMapper;
import com.assignments.rbac.repository.UserRepository;
import com.assignments.rbac.security.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtils jwtUtils;

    @InjectMocks
    private UserService userService;

    private UserRegistrationRequest registrationRequest;
    private LoginRequest loginRequest;
    private User user;
    private UserResponse userResponse;
    private CurrentUserResponse currentUserResponse;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        registrationRequest = new UserRegistrationRequest();
        registrationRequest.setName("Harsh");
        registrationRequest.setUsername("harsh");
        registrationRequest.setEmail("harsh@test.com");
        registrationRequest.setPassword("password123");

        user = new User();
        user.setId(1L);
        user.setName("Harsh");
        user.setUsername("harsh");
        user.setEmail("harsh@test.com");
        user.setPassword("encodedPassword");

        userResponse = new UserResponse();
        userResponse.setId(1L);
        userResponse.setName("Harsh");
        userResponse.setUsername("harsh");
        userResponse.setEmail("harsh@test.com");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("harsh@test.com");
        loginRequest.setPassword("password123");

        currentUserResponse = new CurrentUserResponse();
        currentUserResponse.setId(1L);
        currentUserResponse.setUsername("harsh");
        currentUserResponse.setEmail("harsh@test.com");

        authentication = mock(Authentication.class);
    }

    @Test
    void registerUser_Success() {
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userMapper.toEntity(any(UserRegistrationRequest.class))).thenReturn(user);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toResponse(any(User.class))).thenReturn(userResponse);

        UserResponse result = userService.registerUser(registrationRequest);

        assertNotNull(result);
        assertEquals("Harsh", result.getName());
        assertEquals("harsh", result.getUsername());
        assertEquals("harsh@test.com", result.getEmail());

        verify(userRepository).existsByUsername("harsh");
        verify(userRepository).existsByEmail("harsh@test.com");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerUser_UsernameAlreadyExists() {
        when(userRepository.existsByUsername(anyString())).thenReturn(true);

        UserAlreadyExistsException exception = assertThrows(UserAlreadyExistsException.class, 
            () -> userService.registerUser(registrationRequest));

        assertEquals("Username 'harsh' is already taken", exception.getMessage());
        verify(userRepository).existsByUsername("harsh");
        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerUser_EmailAlreadyExists() {
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        UserAlreadyExistsException exception = assertThrows(UserAlreadyExistsException.class, 
            () -> userService.registerUser(registrationRequest));

        assertEquals("Email 'harsh@test.com' is already in use", exception.getMessage());
        verify(userRepository).existsByUsername("harsh");
        verify(userRepository).existsByEmail("harsh@test.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void loginUser_Success() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtUtils.generateJwtToken(authentication)).thenReturn("jwt-token");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(userMapper.toResponse(any(User.class))).thenReturn(userResponse);

        LoginResponse result = userService.loginUser(loginRequest);

        assertNotNull(result);
        assertEquals("jwt-token", result.getToken());
        assertEquals("Bearer", result.getType());
        assertEquals(userResponse, result.getUser());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtUtils).generateJwtToken(authentication);
        verify(userRepository).findByEmail("harsh@test.com");
    }

    @Test
    void loginUser_UserNotFound() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtUtils.generateJwtToken(authentication)).thenReturn("jwt-token");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(UserNotFoundException.class,
                () -> userService.loginUser(loginRequest));

        assertEquals("User not found with email: harsh@test.com", exception.getMessage());
        verify(userRepository).findByEmail("harsh@test.com");
    }

    @Test
    void loginUser_InvalidCredentials() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        BadCredentialsException exception = assertThrows(BadCredentialsException.class,
                () -> userService.loginUser(loginRequest));

        assertEquals("Invalid email or password", exception.getMessage());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    void getCurrentUser_Success() {
        try (MockedStatic<SecurityContextHolder> mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class)) {
            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("harsh@test.com");
            mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
            when(userMapper.toCurrentUserResponse(any(User.class))).thenReturn(currentUserResponse);

            CurrentUserResponse result = userService.getCurrentUser();

            assertNotNull(result);
            assertEquals("harsh", result.getUsername());
            assertEquals("harsh@test.com", result.getEmail());

            verify(userRepository).findByEmail("harsh@test.com");
            verify(userMapper).toCurrentUserResponse(user);
        }
    }

    @Test
    void getCurrentUser_UserNotFound() {
        try (MockedStatic<SecurityContextHolder> mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class)) {
            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("harsh@test.com");
            mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

            UserNotFoundException exception = assertThrows(UserNotFoundException.class,
                    () -> userService.getCurrentUser());

            assertEquals("User not found with email: harsh@test.com", exception.getMessage());
            verify(userRepository).findByEmail("harsh@test.com");
        }
    }
}
