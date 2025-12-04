package com.assignments.rbac.service;

import com.assignments.rbac.config.RabbitMQConfig;
import com.assignments.rbac.dto.events.UserLoginEvent;
import com.assignments.rbac.dto.events.UserRegistrationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventPublisherService {

    private final RabbitTemplate rabbitTemplate;

    public void publishUserRegistrationEvent(UserRegistrationEvent event) {
        try {
            log.info("Publishing user registration event for user: {} (ID: {})", event.getEmail(), event.getUserId());
            
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.USER_EVENTS_EXCHANGE,
                RabbitMQConfig.USER_REGISTRATION_ROUTING_KEY,
                event
            );
            
            log.debug("User registration event published successfully: {}", event.getEventId());
            
        } catch (Exception e) {
            log.error("Failed to publish user registration event for user: {} - Error: {}", 
                     event.getEmail(), e.getMessage(), e);
        }
    }

    public void publishUserLoginEvent(UserLoginEvent event) {
        try {
            log.info("Publishing user login event for user: {} (ID: {}) - Success: {}", 
                    event.getEmail(), event.getUserId(), event.isLoginSuccessful());
            
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.USER_EVENTS_EXCHANGE,
                RabbitMQConfig.USER_LOGIN_ROUTING_KEY,
                event
            );
            
            log.debug("User login event published successfully: {}", event.getEventId());
            
        } catch (Exception e) {
            log.error("Failed to publish user login event for user: {} - Error: {}", 
                     event.getEmail(), e.getMessage(), e);
        }
    }

    public void publishUserEvent(Object event, String routingKey) {
        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.USER_EVENTS_EXCHANGE,
                routingKey,
                event
            );
            log.debug("Generic user event published with routing key: {}", routingKey);
        } catch (Exception e) {
            log.error("Failed to publish generic user event with routing key: {} - Error: {}", 
                     routingKey, e.getMessage(), e);
        }
    }
}