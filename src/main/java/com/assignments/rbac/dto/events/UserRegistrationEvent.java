package com.assignments.rbac.dto.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistrationEvent {
    
    private String eventId;
    private String eventType = "USER_REGISTRATION";
    private Long userId;
    private String username;
    private String email;
    private String name;
    private LocalDateTime registrationTimestamp;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime eventTimestamp;

    public UserRegistrationEvent(Long userId, String username, String email, String name, 
                                LocalDateTime registrationTimestamp, String ipAddress, String userAgent) {
        this.eventId = java.util.UUID.randomUUID().toString();
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.name = name;
        this.registrationTimestamp = registrationTimestamp;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.eventTimestamp = LocalDateTime.now();
    }
}