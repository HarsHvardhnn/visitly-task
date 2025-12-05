package com.assignments.rbac.controller;

import com.assignments.rbac.dto.CurrentUserResponse;
import com.assignments.rbac.dto.LoginRequest;
import com.assignments.rbac.dto.LoginResponse;
import com.assignments.rbac.dto.UserRegistrationRequest;
import com.assignments.rbac.dto.UserResponse;
import com.assignments.rbac.exception.UserAlreadyExistsException;
import com.assignments.rbac.exception.UserNotFoundException;
import com.assignments.rbac.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class, excludeAutoConfiguration = {org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    private UserRegistrationRequest validRequest;
    private LoginRequest loginRequest;
    private UserResponse userResponse;
    private LoginResponse loginResponse;
    private CurrentUserResponse currentUserResponse;

    @BeforeEach
    void setUp() {
        validRequest = new UserRegistrationRequest();
        validRequest.setName("Harsh");
        validRequest.setUsername("harsh");
        validRequest.setEmail("harsh@test.com");
        validRequest.setPassword("password123");

        userResponse = new UserResponse();
        userResponse.setId(1L);
        userResponse.setName("Harsh");
        userResponse.setUsername("harsh");
        userResponse.setEmail("harsh@test.com");
        userResponse.setCreatedAt(LocalDateTime.now());
        userResponse.setCreatedBy("system");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("harsh@test.com");
        loginRequest.setPassword("password123");

        loginResponse = new LoginResponse("jwt-token", userResponse);

        currentUserResponse = new CurrentUserResponse();
        currentUserResponse.setId(1L);
        currentUserResponse.setUsername("harsh");
        currentUserResponse.setEmail("harsh@test.com");
    }

    @Test
    void registerUser_ValidRequest_ReturnsCreated() throws Exception {
        when(userService.registerUser(any(UserRegistrationRequest.class))).thenReturn(userResponse);

        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("Harsh"))
                .andExpect(jsonPath("$.data.username").value("harsh"))
                .andExpect(jsonPath("$.data.email").value("harsh@test.com"))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    void registerUser_InvalidRequest_MissingName_ReturnsBadRequest() throws Exception {
        validRequest.setName("");

        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerUser_InvalidRequest_MissingUsername_ReturnsBadRequest() throws Exception {
        validRequest.setUsername("");

        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerUser_InvalidRequest_InvalidEmail_ReturnsBadRequest() throws Exception {
        validRequest.setEmail("invalid-email");

        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerUser_InvalidRequest_ShortPassword_ReturnsBadRequest() throws Exception {
        validRequest.setPassword("123");

        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerUser_ServiceThrowsException_ReturnsConflict() throws Exception {
        when(userService.registerUser(any(UserRegistrationRequest.class)))
                .thenThrow(new UserAlreadyExistsException("Username 'harsh' is already taken"));

        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Username 'harsh' is already taken"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void loginUser_ValidCredentials_ReturnsToken() throws Exception {
        when(userService.loginUser(any(LoginRequest.class))).thenReturn(loginResponse);

        mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("jwt-token"))
                .andExpect(jsonPath("$.data.type").value("Bearer"))
                .andExpect(jsonPath("$.data.user.email").value("harsh@test.com"))
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void loginUser_InvalidCredentials_ReturnsUnauthorized() throws Exception {
        when(userService.loginUser(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Invalid email or password"));

        mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Invalid email or password"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void loginUser_InvalidRequest_MissingEmail_ReturnsBadRequest() throws Exception {
        loginRequest.setEmail("");

        mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginUser_InvalidRequest_MissingPassword_ReturnsBadRequest() throws Exception {
        loginRequest.setPassword("");

        mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCurrentUser_Success() throws Exception {
        when(userService.getCurrentUser()).thenReturn(currentUserResponse);

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.username").value("harsh"))
                .andExpect(jsonPath("$.data.email").value("harsh@test.com"))
                .andExpect(jsonPath("$.error").doesNotExist());
    }

}
