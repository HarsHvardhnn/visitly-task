Overall Design Approach

For this assignment, I implemented a User Management System with Role-Based Access Control (RBAC) using Spring Boot 3, MySQL, Spring Security, JWT, and a message broker. My focus was on building something clean, secure, and maintainable — not just functional. I also wanted the system to feel like something that could actually run in production, not just pass a checklist.

The architecture follows the standard layered approach:

Controller → Service → Repository → Database
with security, caching, validation, and audit logging applied as cross-cutting concerns.

Core Database and RBAC Design

To support RBAC cleanly, I went with a normalized three-table model:

users

roles

user_roles (many-to-many mapping)

users Table

The users table stores identity information and basic audit details.

Key decisions:

Both email and username are unique.

Passwords are hashed using BCrypt.

I included fields like last_login_at and soft-delete flags (is_deleted) for real-world operational needs.

roles and user_roles

Roles are defined in the roles table, and user assignments are stored in user_roles.
I intentionally kept the role model simple and flat (e.g., "ADMIN" and "USER") instead of hierarchical, because this is easier to implement and extends well if needed in the future.

Liquibase for Database Versioning

Instead of relying on manual schema creation or Hibernate auto DDL, I used Liquibase for database version control.

Reasons:

It gives me repeatable and trackable schema changes.

It aligns with CI/CD workflows (especially when using Docker).

Makes migrations predictable and environment-safe.

All schema changes — including table creation, auditing fields, and constraints — are managed through Liquibase changelogs.

Security and Authentication
Password Security

Passwords are stored using BCrypt hashing. No plaintext storage or logging.

JWT-Based Authentication

Authentication is stateless and uses JWT.
Tokens include:

User ID

Email

Assigned roles

A configurable expiration window is applied (default ~24h).

Role Enforcement

I used method-level access control (@PreAuthorize) instead of only URL-based rules. This makes protected actions explicit and closer to business logic.

Protected examples:

Creating roles

Assigning roles

Accessing admin-only reports

Layered Architecture & DTOs

I kept the architecture modular:

Controllers only work with DTOs.

Mapping between entities and DTOs is separated (via manual mapping or MapStruct).

Services handle core business logic.

Repositories rely on Spring Data JPA for persistence.

Validation is done using Bean Validation annotations (JSR-380) and invalid input is handled through global exception handling rather than mixed-into controllers.

Swagger/OpenAPI Documentation

To make the API easy to explore and test, I added Swagger/OpenAPI.
This helps with:

Development testing

Onboarding future developers

API discoverability

It also makes it clear what payloads and authorization headers are expected — especially for endpoints requiring JWT tokens.

Caching Strategy (/api/users/me)

Since /api/users/me is likely to be called frequently in a real application (e.g., UI loads), I added caching with Redis (or Spring Cache abstraction).

Key decisions:

A 5-minute TTL balances freshness and performance.

Cache entries are invalidated when roles or profile information changes.

The cache key is based on username/email for consistency.

Messaging

For the messaging component, the service publishes events for two actions:

User registration

Login

These events include basic details like user id, email, timestamp, etc.
This design leaves room for future additions like login analysis, notifications, or analytics without modifying core logic.

Testing Approach

I added integration tests focusing on authorization-sensitive areas:

Role controller (e.g., role creation and assignment)

Admin-only controller endpoints (like system stats)

These tests validate:

Authentication flow

Token authorization rules

HTTP-level behavior

Security boundary enforcement

By testing at the controller level, I ensured the system behaves correctly under real request/response conditions — not just at the service layer.

Error Handling

A @ControllerAdvice-based exception layer ensures consistent error responses.
This avoids exposing stack traces and keeps controllers clean.

Indexing and Performance Considerations

Unique indexes exist on email and username.

The composite key on user_roles ensures efficient role lookups.

Soft delete is combined with JPA filtering so deleted records don’t interfere with active queries.

Assumptions and Trade-offs

Email verification and refresh tokens weren’t included to stay within scope.

JWT revocation isn’t implemented, but the design can support it later.

The RBAC model is intentionally simple but extensible.

Summary

Overall, the architecture emphasizes scalability, clarity, and production readiness:

Layered backend design

Liquibase for schema management

JWT + BCrypt + Spring Security for secure authentication

Role-based authorization enforced at method level

Swagger documentation

Caching and event publishing

Soft deletes and auditing support

The result is a modular system that satisfies the assignment's requirements while being extendable for future real-world scenarios.