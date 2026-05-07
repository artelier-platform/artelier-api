package com.artelier.api.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.info.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(

        info = @Info(
                title = "Artelier API",
                version = "0.1.0",
                description = """
                        Artelier API is a REST backend for an e-commerce platform focused on handmade and artisanal products.

                        **Features:**
                        • JWT authentication and role-based authorization (ADMIN / BUYER)
                        • Product catalog with categories and images
                        • Cloudinary image management
                        • Order system with stock validation
                        • Admin operations for catalog and users
                        • Payment integration via **Wompi** gateway (webhook + signature validation)

                        **Authentication:**
                        Most endpoints require a Bearer JWT token in the `Authorization` header.
                        The only public exception is `POST /api/v1/payments/webhook`, which is
                        secured via Wompi event signature validation (SHA-256 checksum).

                        Architecture follows a modular Spring Boot design based on sprint-driven development.
                        """,

                contact = @Contact(
                        name = "Geronimo Martinez Nuñez",
                        url = "https://github.com/MimiRandomS",
                        email = "geronimo@example.com"
                ),

                license = @License(
                        name = "MIT License",
                        url = "https://opensource.org/licenses/MIT"
                )
        ),

        servers = {
                @Server(
                        description = "Local Development",
                        url = "http://localhost:8080"
                )
                // ,
                // @Server(
                //         description = "Production",
                //         url = "https://api.artelier.com"
                // )
        },

        security = {
                @SecurityRequirement(name = "bearerAuth")
        },

        externalDocs = @ExternalDocumentation(
                description = "Project repository",
                url = "https://github.com/artelier-platform/artelier-api"
        )
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER,
        description = """
                JWT Authorization using Bearer token.

                **Format:**
                `Authorization: Bearer <token>`

                **Example:**
                `Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...`

                Obtain a token via `POST /api/v1/auth/login`.
                """
)
public class OpenApiConfig {
}