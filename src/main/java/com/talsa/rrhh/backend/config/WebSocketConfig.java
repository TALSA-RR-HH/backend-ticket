package com.talsa.rrhh.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Prefijo para los mensajes que salen del servidor hacia el cliente (TV)
        config.enableSimpleBroker("/topic");

        // Prefijo para mensajes que vienen del cliente (si fuera necesario a futuro)
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Punto de conexión: El Frontend se conectará a "http://localhost:8080/ws-tickets"
        registry.addEndpoint("/ws-tickets")
                .setAllowedOriginPatterns("*") // Permite conexión desde cualquier lado (Tablet/TV)
                .withSockJS(); // Habilita compatibilidad si el navegador es viejo
    }
}
