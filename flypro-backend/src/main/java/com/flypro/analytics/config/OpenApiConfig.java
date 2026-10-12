package com.flypro.analytics.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration SpringDoc OpenAPI (Swagger UI) avec schéma Bearer JWT.
 */
@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI flyproOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("FlyPro Analytics API")
                        .description(
                                "API backend de la plateforme intelligente de gestion des badges et des accès. "
                                        + "Phase 1 — Authentication & Authorization.")
                        .version("0.1.0")
                        .contact(new Contact()
                                .name("FlyPro Analytics – PFE")
                                .email("contact@flypro.local"))
                        .license(new License()
                                .name("Academic / PFE")
                                .url("https://flypro.local")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .name(BEARER_AUTH)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT access token obtained from /api/v1/auth/login")));
    }
}
