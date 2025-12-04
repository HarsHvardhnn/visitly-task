package com.assignments.rbac.dto.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserLoginEvent {
    
    private String eventId;
    private String eventType = "USER_LOGIN";
    private Long userId;
    private String username;
    private String email;
    private String name;
    private List<String> roles;
    private LocalDateTime loginTimestamp;
    private String ipAddress;
    private String userAgent;
    private boolean loginSuccessful;
    private String failureReason;
    private LocalDateTime eventTimestamp;

    public UserLoginEvent(Long userId, String username, String email, String name, List<String> roles,
                         LocalDateTime loginTimestamp, String ipAddress, String userAgent, 
                         boolean loginSuccessful, String failureReason) {
        this.eventId = java.util.UUID.randomUUID().toString();
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.name = name;
        this.roles = roles;
        this.loginTimestamp = loginTimestamp;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.loginSuccessful = loginSuccessful;
        this.failureReason = failureReason;
        this.eventTimestamp = LocalDateTime.now();
    }
}