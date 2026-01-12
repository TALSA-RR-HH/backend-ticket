package com.talsa.rrhh.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                // 1. Información General de la API
                .info(new Info()
                        .title("API Tickets RRHH - Talsa")
                        .version("1.0")
                        .description("Documentación del sistema de gestión de colas y tickets para RRHH.")
                        .contact(new Contact()
                                .name("Soporte TI")
                                .email("ti@talsa.com.pe")))
                // 2. Configuración de Seguridad (JWT)
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication", createAPIKeyScheme()));
    }

    private SecurityScheme createAPIKeyScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP) // Tipo HTTP
                .bearerFormat("JWT")            // Formato Token
                .scheme("bearer");              // Esquema Bearer
    }

}
