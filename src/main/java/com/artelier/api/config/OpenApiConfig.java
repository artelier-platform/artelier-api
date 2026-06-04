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
                version = "1.0.0",
                description = """
                        REST backend for **Artelier**, an e-commerce platform for handmade \
                        and artisanal products.

                        ## MVP Scope

                        This release covers the complete backend feature set:

                        | Module | Description |
                        |--------|-------------|
                        | **Authentication** | JWT-based login, registration, token refresh, and logout with refresh-token rotation |
                        | **Authorization** | Role-based access control — `ADMIN` and `BUYER` roles |
                        | **User Moderation** | Admin ban / unban operations |
                        | **Product Catalog** | Full CRUD with category filtering, slug generation, soft delete, and visibility toggle |
                        | **Image Management** | Cloudinary upload and deletion, coordinated by index with per-image metadata |
                        | **Categories** | Public listing and admin creation with unique slug enforcement |
                        | **Order Management** | Order creation with stock validation, role-scoped listing, status transitions, and pagination |
                        | **Payments** | Wompi gateway integration — CARD, PSE, and NEQUI; webhook processing with SHA-256 signature validation |
                        | **Admin Statistics** | Dashboard metrics: monthly sales, pending orders, active products, and top-selling product |
                        | **CI/CD** | Automated pipeline with SonarCloud quality gate and deployment to Render |

                        ## Authentication

                        Most endpoints require a Bearer JWT token in the `Authorization` header.

                        The only intentionally public endpoint is `POST /api/v1/payments/webhook`,
                        which is secured via Wompi SHA-256 event signature validation instead of JWT.

                        ## Stock Types

                        | Type | Behavior |
                        |------|----------|
                        | `UNLIMITED` | No stock tracking |
                        | `AVAILABLE` | Stock tracked and decremented on order |
                        | `MADE_TO_ORDER` | Stock tracked; items produced on demand |

                        ## Order Status Flow
                        
                        PENDING_PAYMENT → PROCESSING → PAID → SHIPPED
                         ↓
                        CANCELLED  (buyer only, from PENDING_PAYMENT)
                        
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
                        description = "Production",
                        url = "https://artelier-api-djt4.onrender.com"
                ),
                @Server(
                        description = "Local Development",
                        url = "http://localhost:8080"
                )
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