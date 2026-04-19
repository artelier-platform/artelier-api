<a id="top"></a>

<p align="center">
  <img src="docs/primary_logo.png" alt="Artelier API" width="100%"/>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white" alt="Java 21"/>
  <img src="https://img.shields.io/badge/Spring_Boot-4.0-6DB33F?logo=springboot&logoColor=white" alt="Spring Boot"/>
  <img src="https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white" alt="PostgreSQL"/>
  <img src="https://img.shields.io/badge/JWT-Auth-black" alt="JWT"/>
  <img src="https://img.shields.io/badge/Cloudinary-Media-3448C5" alt="Cloudinary"/>
  <img src="https://img.shields.io/badge/version-0.1.0--SNAPSHOT-blue" alt="version"/>
  <img src="https://img.shields.io/github/last-commit/artelier-platform/artelier-api" alt="last commit"/>
  <img src="https://img.shields.io/badge/license-MIT-green" alt="license"/>
</p>

<p align="center">
  <b>REST API for Artelier Cajicá — an e-commerce platform for handmade ceramic and wood art.</b><br/>
  Built with Java 21 · Spring Boot · PostgreSQL · JWT · Cloudinary
</p>

---

## Table of Contents

- [About the Project](#-about-the-project)
- [Tech Stack](#-stack-backend)
- [Architecture Overview](#-architecture-overview)
- [Testing](#-testing--quality)
- [Getting Started](#-getting-started)
- [Environment Variables](#-environment-variables)
- [Deploy](#-deployment--infrastructure)
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

This is also a full-stack portfolio project demonstrating a production-grade architecture with Spring Boot, JWT auth, Flyway migrations, Cloudinary integration, and CI/CD.

---

## 🚀 Stack Backend

| Layer | Technology |
|-------|-------------|
| **Language** | ![Java](https://img.shields.io/badge/Java-21-007396?logo=openjdk&logoColor=white) |
| **Frameworks** | ![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0-6DB33F?logo=springboot&logoColor=white) ![Spring Security](https://img.shields.io/badge/Spring_Security-JWT-6DB33F?logo=springsecurity&logoColor=white) ![Spring Cache](https://img.shields.io/badge/Spring_Cache-Enabled-6DB33F?logo=spring&logoColor=white) |
| **Persistence** | ![Spring Data JPA](https://img.shields.io/badge/Spring_Data_JPA-Hibernate-59666C?logo=hibernate&logoColor=white) ![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white) ![Flyway](https://img.shields.io/badge/Flyway-Migrations-CC0200?logo=flyway&logoColor=white) |
| **Infrastructure** | ![Bucket4j](https://img.shields.io/badge/Bucket4j-8.18.0-blue) ![Cloudinary](https://img.shields.io/badge/Cloudinary-Storage-3448C5?logo=cloudinary&logoColor=white) |
| **Utilities** | ![MapStruct](https://img.shields.io/badge/MapStruct-1.6.3-orange) ![Lombok](https://img.shields.io/badge/Lombok-Enabled-red) ![dotenv](https://img.shields.io/badge/dotenv-Config-ECD53F?logo=dotenv&logoColor=black) |
| **Docs & Monitoring** | ![Swagger](https://img.shields.io/badge/Swagger-OpenAPI-85EA2D?logo=swagger&logoColor=black) ![Spring Actuator](https://img.shields.io/badge/Spring_Actuator-Monitoring-6DB33F?logo=springboot&logoColor=white) |
---

## 🏗 Architecture Overview

![Artelier API — Architecture.svg](docs/Artelier%20API%20%E2%80%94%20Architecture.svg)
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

## 🧪 Testing & Quality

### Testing Stack

<p align="center">

<img src="https://img.shields.io/badge/JUnit_5-Test_Framework-25A162?logo=junit5&logoColor=white" alt="JUnit 5"/>
<img src="https://img.shields.io/badge/Mockito-Mocking-78A641?logo=mockito&logoColor=white" alt="Mockito"/>
<img src="https://img.shields.io/badge/Spring_MockMvc-HTTP_Testing-6DB33F?logo=spring&logoColor=white" alt="Spring MockMvc"/>
<img src="https://img.shields.io/badge/JaCoCo-Code_Coverage-BF1E2E" alt="JaCoCo"/>

</p>

### Code Quality Metrics

<p align="center">

<img src="https://sonarcloud.io/api/project_badges/measure?project=MimiRandomS_artelier-api&metric=alert_status" alt="Quality Gate"/>
<img src="https://sonarcloud.io/api/project_badges/measure?project=MimiRandomS_artelier-api&metric=coverage" alt="Coverage"/>
<img src="https://sonarcloud.io/api/project_badges/measure?project=MimiRandomS_artelier-api&metric=bugs" alt="Bugs"/>
<img src="https://sonarcloud.io/api/project_badges/measure?project=MimiRandomS_artelier-api&metric=vulnerabilities" alt="Vulnerabilities"/>
<img src="https://sonarcloud.io/api/project_badges/measure?project=MimiRandomS_artelier-api&metric=code_smells" alt="Code Smells"/>
<img src="https://sonarcloud.io/api/project_badges/measure?project=MimiRandomS_artelier-api&metric=duplicated_lines_density" alt="Duplications"/>

</p>

The project includes a **comprehensive automated test suite** covering both  
**service and controller layers**, with coverage tracked via **JaCoCo**  
and static analysis performed using **SonarCloud**.

### Running Tests

```bash
# Run all tests
./mvnw test

# Run with coverage report
./mvnw verify

# Open the coverage report
open target/site/jacoco/index.html
```

The JaCoCo report is generated at `target/site/jacoco/index.html` after running `mvn verify`.

---

## 🚀 Getting Started

### Prerequisites

![Java](https://img.shields.io/badge/Java-21-007396?logo=openjdk&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Containerization-2496ED?logo=docker&logoColor=white)
![Docker Compose](https://img.shields.io/badge/Docker_Compose-Orchestration-2496ED?logo=docker&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-3.9+-C71A36?logo=apachemaven&logoColor=white)

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

## ☁️ Deployment & Infrastructure

### Platforms

<p align="center">

<img src="https://img.shields.io/badge/Backend-Render-46E3B7?logo=render&logoColor=black" alt="Render Backend"/>
<img src="https://img.shields.io/badge/Database-Supabase_PostgreSQL-3ECF8E?logo=supabase&logoColor=white" alt="Supabase Database"/>
<img src="https://img.shields.io/badge/Frontend-Vercel-black?logo=vercel&logoColor=white" alt="Vercel Frontend"/>
<img src="https://img.shields.io/badge/Media-Cloudinary-3448C5?logo=cloudinary&logoColor=white" alt="Cloudinary Media"/>

</p>

### Deployment Notes

- **Backend:** Deployed on **Render** with automatic deploys from the `main` branch
- **Database:** Managed **Supabase PostgreSQL** instance
- **Frontend:** Hosted on **Vercel** with automatic deploys from `main`
- **Media Storage:** Images handled via **Cloudinary** (free tier)

**Database migrations** are handled automatically using **Flyway**  
during application startup — no manual database setup required  
beyond providing the connection variables.
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
  <img 
    src="https://avatars.githubusercontent.com/MimiRandomS" 
    width="120" 
    alt="MimiRandomS GitHub avatar"
    style="border-radius:50%"
  />
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

