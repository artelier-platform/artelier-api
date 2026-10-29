package com.artelier.api.config;

import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * Global OpenAPI configuration for the Artelier API.
 *
 * <p>This class holds no logic: it only carries the annotations that
 * springdoc turns into the top-level sections of the generated OpenAPI
 * document.</p>
 *
 * <ul>
 *   <li>{@link OpenAPIDefinition}: API metadata ({@code info}), the list of
 *       {@code servers}, the default {@code security} requirement and the
 *       external documentation link.</li>
 *   <li>{@link SecurityScheme}: definition of the {@code bearerAuth} scheme
 *       (JWT sent in the {@code Authorization} header).</li>
 * </ul>
 *
 * <h2>Security model</h2>
 * <p>The {@code bearerAuth} requirement is applied <b>globally</b>, so every
 * operation is documented as protected by default. Public endpoints opt out
 * individually with
 * {@link io.swagger.v3.oas.annotations.security.SecurityRequirements}
 * (an empty requirement list) on the controller method.</p>
 *
 * <p>Per-endpoint documentation lives in each controller; this class only
 * documents what is common to the whole API.</p>
 */
@Configuration
@OpenAPIDefinition(

        info = @Info(
                title = "Artelier API",
                version = "1.0.0",
                description = """
                        REST backend for **Artelier**, an e-commerce platform for handmade \
                        and artisanal products.

                        ## Links

                        | Resource | URL |
                        |----------|-----|
                        | **Frontend** | https://artelier-web.vercel.app/ |
                        | **API repository** | https://github.com/artelier-platform/artelier-api |

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
                        | **Banners** | Public listing of the active storefront banners |
                        | **Order Management** | Order creation with stock validation, role-scoped listing, status transitions, and pagination |
                        | **Payments** | Wompi gateway integration — CARD, PSE, and NEQUI; webhook processing with SHA-256 signature validation |
                        | **Admin Statistics** | Dashboard metrics: monthly sales, pending orders, active products, and top-selling product |
                        | **CI/CD** | Automated pipeline with SonarCloud quality gate and deployment to Render |

                        ## Authentication

                        Protected endpoints require a Bearer JWT token in the `Authorization` header.

                        The following endpoints are **public** and do not require a token:

                        - Authentication: register, login, refresh and logout
                        - Public catalog reads: products, categories and banners
                        - `POST /api/v1/payments/webhook`, which is secured via Wompi SHA-256
                          event signature validation instead of JWT

                        Everything else requires a valid access token. Admin endpoints additionally
                        require the `ADMIN` role.

                        ## Response Format

                        Every JSON response is wrapped in the same envelope:

                        | Field | Description |
                        |-------|-------------|
                        | `success` | `true` when the request was processed correctly |
                        | `message` | Human-readable summary of the result |
                        | `data` | Payload of the response (when applicable) |

                        Errors use the same envelope with `success = false`.

                        ## Stock Types

                        | Type | Behavior |
                        |------|----------|
                        | `UNLIMITED` | No stock tracking |
                        | `AVAILABLE` | Stock tracked and decremented on order |
                        | `MADE_TO_ORDER` | Stock tracked; items produced on demand |

                        ## Order Status Flow

                        | Transition | Triggered by | Who |
                        |------------|--------------|-----|
                        | → `PENDING_PAYMENT` | Order created | Buyer |
                        | `PENDING_PAYMENT` → `PROCESSING` | Payment initiated | System |
                        | `PROCESSING` → `PAID` | Payment approved | System |
                        | `PROCESSING` → `PENDING_PAYMENT` | Payment declined or voided | System |
                        | `PAID` → `SHIPPED` | Manual status update | Admin only |
                        | `PENDING_PAYMENT` → `CANCELLED` | Manual status update | Buyer only (own order) |
                        """,

                contact = @Contact(
                        name = "Geronimo Martinez Nuñez",
                        url = "https://github.com/MimiRandomDev"
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