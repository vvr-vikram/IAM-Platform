# Enterprise Identity & Access Management (IAM) Platform

A production-ready Identity & Access Management (IAM) platform built with Java 21, Spring Boot 3.2.5, Spring Security, MySQL, Redis, Flyway migrations, and OpenAPI/Swagger documentation.

---

## Features
- **User Management**: Lifecycle operations, registration, activation.
- **Email Verification & Password Recovery**: Uses token-based verification (simulated and logged to console for ease of testing).
- **JWT Authentication**: Secure stateless access tokens and long-lived refresh tokens.
- **Role-Based Access Control (RBAC)**: Support for permissions (`user:read`, `user:write`, `admin:read`, `admin:write`, `audit:read`) mapped to roles (`ROLE_USER`, `ROLE_MANAGER`, `ROLE_ADMIN`).
- **Multi-Factor Authentication (MFA)**: Built-in TOTP (Time-based One-time Password) compatible with Google Authenticator.
- **Account Lockout**: Locks account for 15 minutes after 5 consecutive failed login attempts.
- **Audit Logging**: Logs all security events to the database with client IP address and user-agent metadata.
- **Session Management & Token Revocation**: Revokes refresh tokens and blacklists active access tokens in Redis.
- **Admin Management APIs**: Paginated user directories, manual lockout/unlock controls, role mapping, audit logs view, active sessions monitoring and revocation.

---

## Technical Stack
- **Language**: Java 21
- **Framework**: Spring Boot 3.2.5
- **Security**: Spring Security & Spring Security Test
- **Database**: MySQL 8.0 & Hibernate
- **Caching**: Redis & Spring Data Redis (with automatic in-memory fallback for local testing when Redis is offline)
- **Migrations**: Flyway Migration
- **API Doc**: OpenAPI 3.0 / SpringDoc Swagger UI

---

## Project Structure
- `com.enterprise.iam.config`: Security filters, CORS/CSRF configurations, Swagger documentation, and Redis fallback setup.
- `com.enterprise.iam.controller`: REST Controllers for Authentication, User profile, and Administration.
- `com.enterprise.iam.dto`: Immutable/Validated Request and Response payloads.
- `com.enterprise.iam.entity`: Database entity mappings (User, Role, Permission, RefreshToken, AuditLog).
- `com.enterprise.iam.exception`: Global exception handler mapping validation and security errors.
- `com.enterprise.iam.repository`: Spring Data JPA repositories.
- `com.enterprise.iam.service`: Core IAM business logic (MFA, JWT, Lockout, and Auditing).

---

## Getting Started

### Prerequisites
- **Java**: JDK 21+ (Compatible up to JDK 26)
- **Database**: MySQL Server running on `localhost:3306`
- **Cache**: Redis Server running on `localhost:6379` (Optional; application will fallback to in-memory cache automatically if unavailable)

---

## Local Installation & Run

1. **Database Setup**:
   Create a database named `iam_db` in your MySQL:
   ```sql
   CREATE DATABASE iam_db;
   ```
   *Note: Default credentials configured are username `root` and password `1254`. You can change these inside `src/main/resources/application.yml` or inject them as environment variables.*

2. **Eclipse Integration**:
   Import the project into Eclipse:
   - File -> Import -> Existing Maven Projects.
   - Select the `iam-platform` root directory.

3. **Running the Application**:
   Run the application from your terminal or inside Eclipse:
   ```bash
   .\mvnw.cmd spring-boot:run
   ```
   Or run the main class `com.enterprise.iam.IamApplication`.

---

## Docker Deployment (Docker Compose)
To run the entire platform with preconfigured MySQL, Redis, and App containers:

1. Build and boot the services:
   ```bash
   docker-compose up -d --build
   ```
2. Verify all services are healthy and running:
   ```bash
   docker-compose ps
   ```
3. Stop the services:
   ```bash
   docker-compose down
   ```

---

## API Documentation & Swagger UI
Once the application is running, access the interactive Swagger UI to review and test the APIs:
- **Swagger Link**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

---

## REST Endpoints Overview

### 1. Public Authentication (`/api/v1/auth`)
- `POST /register`: Registers a new user account.
- `POST /verify-email`: Activates the account using the OTP code logged in the console.
- `POST /login`: Logs in using username and password. Returns JWTs OR redirects to MFA if enabled.
- `POST /login/mfa`: Submits the TOTP code if login returned `mfaRequired`.
- `POST /refresh`: Retrieves a new access token using a valid refresh token.
- `POST /logout`: Revokes the refresh token and blacklists the current access token.
- `POST /forgot-password`: Requests a password reset OTP.
- `POST /reset-password`: Resets the password using the OTP code logged in the console.

### 2. User Profiles (`/api/v1/users`)
- `GET /me`: Returns the logged-in user profile.
- `POST /me/password`: Changes the user password.
- `POST /me/mfa/setup`: Sets up MFA. Returns a TOTP secret and QR URL.
- `POST /me/mfa/enable`: Enforces MFA on login by validating a TOTP code.
- `POST /me/mfa/disable`: Disables MFA.

### 3. Administration APIs (`/api/v1/admin`)
- `GET /users`: Paginated view of all registered users (Requires `admin:read` / Admin role).
- `PUT /users/{userId}/status`: Lock/unlock or enable/disable accounts (Requires `admin:write`).
- `PUT /users/{userId}/roles`: Assign roles (e.g., `ROLE_ADMIN`, `ROLE_USER`) (Requires `admin:write`).
- `GET /audit-logs`: View all platform activity audit records (Requires `audit:read`).
- `GET /sessions`: View active login sessions (Requires `admin:read`).
- `POST /sessions/{sessionId}/revoke`: Remotely terminate a user session (Requires `admin:write`).

---

## Initial Database Seeds
On startup, Flyway automatically seeds the database:
- **Default Administrator Credentials**:
  - **Username**: `admin`
  - **Password**: `Admin@123`
  - **Roles**: `ROLE_ADMIN` (fully active and email verified)
