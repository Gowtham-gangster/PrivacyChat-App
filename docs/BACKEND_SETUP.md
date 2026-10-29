# PrivyChat Backend Setup & Execution Guide

## 1. Prerequisites

- **Java Development Kit (JDK)**: OpenJDK 21 or 24
- **Build Tool**: Apache Maven 3.9+
- **Database**: PostgreSQL 16 (or local Docker container)
- **Object Storage**: S3-compatible service (MinIO locally or AWS S3 / Cloudflare R2)

---

## 2. Local Database & Services Setup via Docker

Run the preconfigured services in `infrastructure/docker`:

```powershell
cd infrastructure/docker
docker compose up -d
```

This starts:
- **PostgreSQL 16**: Port `5432`, Database `privychat`, User `privychat_user`, Password `privychat_password`
- **MinIO**: API Port `9000`, Web Console `9001` (User: `minioadmin`, Password: `minioadmin`)
- **Coturn**: Port `3478` (STUN / TURN)

---

## 3. Database Migrations (Flyway)

Migrations are automatically applied on Spring Boot startup from `src/main/resources/db/migration/`:
- `V1__init_schema.sql`: Initializes all core tables (`users`, `user_devices`, `device_prekey_bundles`, `one_time_prekeys`, `conversations`, `conversation_participants`, `messages`, `encrypted_attachments`, `invitations`, `call_sessions`, `blocked_users`).
- `V2__indexes_and_constraints.sql`: Applies query performance indexes and database constraints.

To run or repair migrations manually via Maven:
```powershell
mvn flyway:migrate
```

---

## 4. Running the Backend

### Local Development Mode
```powershell
cd backend
mvn clean compile spring-boot:run -Dspring-boot.run.profiles=dev
```

### Running Tests
```powershell
mvn clean test
```

### Accessing APIs & Diagnostics
- **REST Base URL**: `http://localhost:8080/api/v1`
- **Swagger UI Interactive Documentation**: `http://localhost:8080/swagger-ui/index.html`
- **OpenAPI JSON Spec**: `http://localhost:8080/v3/api-docs`
- **Actuator Health & Metrics**: `http://localhost:8080/actuator/health`
- **WebSocket STOMP Endpoint**: `ws://localhost:8080/ws`

