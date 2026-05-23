package com.example.backendastramaco.security.swaggerConfig;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Astramaco - Sistema de Gestión de Cargas")
                        .version("2.0.0")
                        .description("""
                                ## API para el Sistema de Gestión de Cargas Astramaco
                                
                                Esta API permite gestionar el transporte de tarjetas de diferentes tipos,
                                asignación de cargas a transportistas, y seguimiento de inventario.
                                
                                ### Características principales:
                                * Gestión de transportistas
                                * Asignación y seguimiento de cargas
                                * Sistema de auditoría completa
                                * Autenticación JWT
                                * Gestión de documentos personales
                                * Pedidos y entregas
                                
                                ### Roles:
                                * **ADMIN**: Acceso total a todas las operaciones
                                * **TRANSPORTISTA**: Acceso a sus propias cargas, pedidos y documentos
                                """)
                        .contact(new Contact()
                                .name("Soporte Astramaco")
                                .email("soporte@astramaco.com")
                                .url("https://astramaco.com"))
                        .license(new License()
                                .name("Licencia Propietaria")
                                .url("https://astramaco.com/license")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"));
    }
}