package com.assignments.rbac.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String USER_EVENTS_QUEUE = "user.events.queue";
    public static final String USER_REGISTRATION_QUEUE = "user.registration.queue";
    public static final String USER_LOGIN_QUEUE = "user.login.queue";

    public static final String USER_EVENTS_EXCHANGE = "user.events.exchange";

    public static final String USER_REGISTRATION_ROUTING_KEY = "user.registration";
    public static final String USER_LOGIN_ROUTING_KEY = "user.login";

    @Bean
    public TopicExchange userEventsExchange() {
        return new TopicExchange(USER_EVENTS_EXCHANGE);
    }

    @Bean
    public Queue userEventsQueue() {
        return QueueBuilder.durable(USER_EVENTS_QUEUE).build();
    }

    @Bean
    public Queue userRegistrationQueue() {
        return QueueBuilder.durable(USER_REGISTRATION_QUEUE).build();
    }

    @Bean
    public Queue userLoginQueue() {
        return QueueBuilder.durable(USER_LOGIN_QUEUE).build();
    }

    @Bean
    public Binding userRegistrationBinding() {
        return BindingBuilder
                .bind(userRegistrationQueue())
                .to(userEventsExchange())
                .with(USER_REGISTRATION_ROUTING_KEY);
    }

    @Bean
    public Binding userLoginBinding() {
        return BindingBuilder
                .bind(userLoginQueue())
                .to(userEventsExchange())
                .with(USER_LOGIN_ROUTING_KEY);
    }

    @Bean
    public Binding userEventsBinding() {
        return BindingBuilder
                .bind(userEventsQueue())
                .to(userEventsExchange())
                .with("user.*");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}