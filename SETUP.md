# RBAC Project Setup

This document outlines the setup completed for the RBAC (Role-Based Access Control) project.

## Technologies Configured

### 1. Spring Security
- **BCrypt Password Encoding**: Configured for secure password hashing
- **JWT Authentication**: Token-based authentication system
- **Method-level Security**: Enabled with `@EnableMethodSecurity`

### 2. Database Setup
- **MySQL**: Primary database configured
- **Spring Data JPA**: For database operations
- **Hibernate**: ORM framework with MySQL dialect

### 3. Liquibase
- **Database Migrations**: Configured for version-controlled database changes
- **Master Changelog**: Located at `src/main/resources/db/changelog/db.changelog-master.xml`

## Configuration Files

### Application Properties
```properties
# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/rbac_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=password

# JPA/Hibernate Configuration
spring.jpa.hibernate.ddl-auto=none
spring.jpa.show-sql=true

# Liquibase Configuration
spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.xml

# JWT Configuration
jwt.secret=mySecretKey
jwt.expiration=86400000
```

### Dependencies Added
- `mysql-connector-java`: MySQL database driver
- `jjwt-api`, `jjwt-impl`, `jjwt-jackson`: JWT token handling
- `liquibase-core`: Database migration management
- `spring-boot-starter-validation`: Input validation

## Security Configuration

### JWT Components
1. **JwtUtils**: Token generation and validation
2. **AuthEntryPointJwt**: Handles unauthorized access
3. **AuthTokenFilter**: Processes JWT tokens in requests

### Endpoints Configuration
- `/api/auth/**`: Public (authentication endpoints)
- `/api/public/**`: Public (no authentication required)
- All other endpoints: Require authentication

## Test Endpoints
- **GET** `/api/public/test`: Public endpoint (no auth required)
- **GET** `/api/private/test`: Private endpoint (JWT token required)

## Next Steps

1. **Database Setup**: Create MySQL database `rbac_db`
2. **User Management**: Create User entity and repository
3. **Role Management**: Create Role entity and RBAC system
4. **Authentication Controller**: Implement login/register endpoints
5. **Liquibase Migrations**: Create database tables using Liquibase changesets

## Important Notes

- **JWT Secret**: Change the JWT secret in production
- **Database Credentials**: Update MySQL credentials as needed
- **H2 Console**: Currently enabled for testing (remove in production)
- **Liquibase**: Ready to use - just add changelog files to create tables

