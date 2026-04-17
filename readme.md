<a name="top"></a>

<p align="center">
  <img src="https://raw.githubusercontent.com/MimiRandomS/artelier-api/main/.github/banner.png" alt="Artelier API" width="100%"/>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white" alt="Java 21"/>
  <img src="https://img.shields.io/badge/Spring_Boot-4.0-6DB33F?logo=springboot&logoColor=white" alt="Spring Boot"/>
  <img src="https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white" alt="PostgreSQL"/>
  <img src="https://img.shields.io/badge/JWT-Auth-black" alt="JWT"/>
  <img src="https://img.shields.io/badge/Cloudinary-Media-blue" alt="Cloudinary"/>
  <img src="https://img.shields.io/badge/version-0.1.0--SNAPSHOT-blue" alt="version"/>
  <img src="https://img.shields.io/github/last-commit/MimiRandomS/artelier-api" alt="last commit"/>
  <img src="https://img.shields.io/badge/coverage-0%25-lightgrey" alt="coverage"/>
  <img src="https://img.shields.io/badge/license-MIT-green" alt="license"/>
</p>

<p align="center">
  <b>REST API for Artelier Cajicá — an e-commerce platform for handmade ceramic and wood art.</b><br/>
  Built with Java 21 · Spring Boot · PostgreSQL · JWT · Cloudinary
</p>

---

## Table of Contents

- [About the Project](#-about-the-project)
- [Tech Stack](#-tech-stack)
- [Architecture Overview](#-architecture-overview)
- [Getting Started](#-getting-started)
- [Environment Variables](#-environment-variables)
- [Deploy](#-deploy)
- [Contributing](#-contributing)
- [License](#-license)
- [Author](#-author)

---

## 🎨 About the Project

**Artelier Cajicá** is a real handmade art brand based in Cajicá, Colombia. The brand sells one-of-a-kind ceramic and wood sculptures — each piece hand-painted, some made entirely from scratch using clay.

Up until now, sales have been mostly word-of-mouth. The artist has an Instagram account ([@arteliercajica](https://www.instagram.com/arteliercajica)) with 109 posts but limited reach. This platform was built to solve that: a dedicated online store that showcases her work properly, handles orders and payments, and gives her a real digital presence.

**What this project solves:**

- Replaces DM-based ordering with a structured shopping flow
- Handles mixed-inventory products — both in-stock pieces and made-to-order work
- Supports Colombian payment methods: Nequi, Bancolombia, Daviplata, PSE via **Wompi**
- Gives the artist an admin dashboard to manage products, orders, and media uploads
- Lays the groundwork for future content like process videos and workshop bookings

This is also a full-stack portfolio project demonstrating a production-grade architecture with Spring Boot, JWT auth, cache management, Flyway migrations, Cloudinary integration, and CI/CD.

---

## 🛠 Tech Stack

### Backend
| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.0 (WebMVC) |
| ORM | Spring Data JPA + Hibernate |
| Database | PostgreSQL 16 |
| Migrations | Flyway |
| Auth | Spring Security + JWT (jjwt 0.12.6) |
| Cache | Spring Cache |
| Rate Limiting | Bucket4j 8.18.0 |
| Image Storage | Cloudinary (cloudinary-http5 2.3.2) |
| Mapping | MapStruct 1.6.3 |
| Boilerplate | Lombok |
| Docs | SpringDoc OpenAPI 3.0.2 (Swagger UI) |
| Monitoring | Spring Actuator |
| Dev Config | springboot3-dotenv |

---

## 🏗 Architecture Overview

The backend follows a layered architecture based on Spring Boot best practices:

- **Controller Layer** → Exposes REST endpoints (public and admin APIs)
- **Service Layer** → Contains business logic and application rules
- **Repository Layer** → Data access using Spring Data JPA
- **Entity Layer** → JPA domain models and database mappings
- **DTO Layer** → Request/response models for API communication
- **Mapper Layer** → MapStruct-based transformations between entities and DTOs
- **Security Layer** → JWT authentication, authorization filters and configurations
- **Exception Layer** → Centralized error handling and custom exceptions
- **Config Layer** → Application configuration (CORS, OpenAPI, Cloudinary, Cache, etc.)

### Package Structure

```
com.artelier.api
├── config        # Application configuration (OpenAPI, security, etc.)
├── controller    # REST controllers
├── dto           # Request / Response models
├── entity        # JPA entities and enums
├── exception     # Global exception handling
├── mapper        # MapStruct mappers
├── repository    # Data access layer
├── security      # JWT and Spring Security
└── service       # Business logic
    └── impl      # Service implementations
```

---

## 🚀 Getting Started

### Prerequisites

- Java 21
- Docker + Docker Compose
- Maven 3.9+

### 1. Clone the repo

```bash
git clone https://github.com/MimiRandomS/artelier-api.git
cd artelier-api
```

### 2. Configure environment

```bash
cp .env.example .env
# Fill in your values — see Environment Variables section below
```

### 3. Start the database

```bash
docker compose up -d db
```

### 4. Run the API

```bash
./mvnw spring-boot:run
```

The API will be available at `http://localhost:8080`.  
Swagger UI: `http://localhost:8080/swagger-ui.html`

### 5. Full local stack (optional)

```bash
docker compose up -d
```

This spins up PostgreSQL + the Spring Boot app together.

---

## ⚙️ Environment Variables

Copy `.env.example` and fill in the following:

```env
# App
APP_NAME=artelier-api
SERVER_PORT=8080

# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=artelier
DB_USERNAME=postgres
DB_PASSWORD=postgres

# JWT
JWT_SECRET=change_this_to_a_very_long_secure_secret_key_123456
JWT_EXPIRATION_MS=900000           # 15 minutes
JWT_REFRESH_EXPIRATION_DAYS=7

# Cloudinary
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_API_KEY=your_api_key
CLOUDINARY_API_SECRET=your_api_secret

# CORS
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173

# Flyway
FLYWAY_ENABLED=true

# JPA
JPA_DDL_AUTO=validate
JPA_SHOW_SQL=false

# Swagger
SWAGGER_ENABLED=true

# Actuator
ACTUATOR_EXPOSE=health,info,metrics
```

> ⚠️ Never commit your real `.env`. It is already in `.gitignore`.

---

## ☁️ Deploy

| Environment | Platform | Notes |
|---|---|---|
| Backend | Railway | Auto-deploy from `main` branch |
| Database | Railway PostgreSQL | Managed, same project |
| Frontend | Vercel | Auto-deploy from `main` branch |
| Media | Cloudinary | Free tier for dev |

Flyway runs automatically on startup — no manual DB setup needed in production beyond providing the connection variables.

---

## 🤝 Contributing

This is a personal portfolio project, but feedback and suggestions are welcome.

1. Fork the repo
2. Create a feature branch: `git checkout -b feat/my-feature`
3. Commit your changes: `git commit -m 'feat: add my feature'`
4. Push and open a Pull Request

Please follow [Conventional Commits](https://www.conventionalcommits.org/) for commit messages.

---

## 📃 License

Distributed under the MIT License. See `LICENSE` for details.

---

## 👨‍💻 Author

<p align="center">
  <img src="https://avatars.githubusercontent.com/MimiRandomS" width="120" style="border-radius:50%"/>
</p>

<p align="center">
  <b>Geronimo Martinez Nuñez</b><br/>
  Systems Engineering Student · Full Stack Developer
</p>

I focus on building scalable backend systems, distributed architectures, and real-world applications that solve practical problems.

This project reflects my approach to backend development — designing clean, maintainable APIs with proper architecture, security, and integration of external services.

### 🔧 What I work with
- Backend development (Java · Spring Boot)
- RESTful APIs and real-time systems
- Cloud-based architectures and deployment
- Automation workflows and AI-assisted solutions

### 🌐 Links
- GitHub: https://github.com/MimiRandomS

---

<p align="center">
  Built with ❤️ for <a href="https://www.instagram.com/arteliercajica">Artelier Cajicá</a> — handmade art from Cajicá, Colombia.
</p>

[Back to top](#top)