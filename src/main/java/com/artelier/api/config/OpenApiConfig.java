package com.artelier.api.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.info.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(

        info = @Info(
                title = "Artelier API",
                version = "1.0.0",
                description = """
                        REST API for Artelier e-commerce platform.

                        This API provides endpoints for:

                        • User authentication (JWT based)
                        • Product catalog browsing
                        • Product management (ADMIN only)
                        • Category organization
                        • Image management via Cloudinary

                        All protected endpoints require a valid Bearer JWT token.
                        """,

                termsOfService = "https://artelier.com/terms",

                contact = @Contact(
                        name = "Artelier Development Team",
                        email = "dev@artelier.com",
                        url = "https://artelier.com"
                ),

                license = @License(
                        name = "Apache 2.0",
                        url = "https://www.apache.org/licenses/LICENSE-2.0"
                )
        ),

        servers = {

                @Server(
                        description = "Local Development Server",
                        url = "http://localhost:8080"
                ),

                @Server(
                        description = "Staging Server",
                        url = "https://staging.api.artelier.com"
                ),

                @Server(
                        description = "Production Server",
                        url = "https://api.artelier.com"
                )
        },

        security = {
                @SecurityRequirement(name = "bearerAuth")
        },

        tags = {

                @Tag(
                        name = "Authentication",
                        description = "User authentication and session management"
                ),

                @Tag(
                        name = "Products",
                        description = "Product catalog operations"
                ),

                @Tag(
                        name = "Admin",
                        description = "Administrative operations"
                )

        },

        externalDocs = @ExternalDocumentation(
                description = "Full API Documentation",
                url = "https://docs.artelier.com"
        )

)

@SecurityScheme(
        name = "bearerAuth",
        description = """
                JWT Authorization header using Bearer scheme.

                Example:
                Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
                """,
        scheme = "bearer",
        bearerFormat = "JWT",
        type = SecuritySchemeType.HTTP,
        in = SecuritySchemeIn.HEADER
)

public class OpenApiConfig {
}