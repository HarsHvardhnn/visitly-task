RBAC User Management System

A comprehensive Role-Based Access Control (RBAC) system built with Spring Boot 3, featuring user authentication, role management, and admin capabilities. This application uses JWT for stateless authentication, MySQL for data persistence, and RabbitMQ for event messaging.

For detailed architecture and design decisions, see [designDetailsAndArchitecture.md](./designDetailsAndArchitecture.md).

What's Included

- User Registration & Authentication: Secure user signup and login with JWT tokens
- Role Management: Create roles and assign them to users (admin-only)
- Admin Dashboard: View system statistics and user details
- API Documentation: Interactive Swagger UI for exploring all endpoints
- Event Messaging: RabbitMQ integration for user registration and login events
- Audit Trail: Automatic tracking of who created/updated records and when

Prerequisites

For Docker Setup:
- Docker Desktop (or Docker Engine + Docker Compose)
- At least 4GB of free RAM
- Ports 8080, 3307, 5673, and 15673 available

For Local Maven Setup:
- Java 17 or higher
- Maven 3.6+ (or use the included Maven wrapper)
- MySQL 8.0+ running locally on port 3306
- RabbitMQ running locally on port 5672 (optional, but recommended)

Quick Start with Docker

This is the easiest way to get everything running. Docker Compose will spin up three containers: the Spring Boot application, MySQL database, and RabbitMQ message broker.

Step 1: Build and Start Everything

Run this command:
docker-compose up --build

This command will:
1. Pull the MySQL 8.0 and RabbitMQ images
2. Build the Spring Boot application from source
3. Start all three services in the correct order
4. Wait for MySQL and RabbitMQ to be healthy before starting the app
5. Run Liquibase migrations to set up the database schema

The first time you run this, it might take a few minutes to download images and build the application. Subsequent runs will be much faster.

Step 2: Verify Everything is Running

Once you see logs indicating the application has started, you can verify:

- Application: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- RabbitMQ Management: http://localhost:15673 (username: admin, password: admin123)
- MySQL: Available on localhost:3307 (to avoid conflicts with local MySQL)

Step 3: Stop Everything

When you're done, stop all containers:
docker-compose down

To also remove volumes (this will delete your database data):
docker-compose down -v

Running Locally with Maven

If you prefer to run everything locally without Docker, follow these steps:

Step 1: Set Up MySQL

Make sure MySQL is running and create the database:
CREATE DATABASE rbac_db;

Update the database credentials in src/main/resources/application.properties if needed (default is root/root).

Step 2: Set Up RabbitMQ (Optional)

If you have RabbitMQ installed locally, make sure it's running on port 5672. If you don't have RabbitMQ, the application will still work, but event publishing will fail silently.

Step 3: Build the Application

Using the Maven wrapper (recommended):
./mvnw clean package

Or with a local Maven installation:
mvn clean package

Step 4: Run the Application

./mvnw spring-boot:run


The application will start on http://localhost:8080

Default Admin Credentials

A default admin user is automatically created when the database is initialized. You can use these credentials to log in and test admin endpoints:

Email: admin@rbac.com
Password: harsh123

This user has the ADMIN role and can:
- Create new roles
- Assign roles to users
- View admin statistics
- Access all protected endpoints


API Documentation

Once the application is running, you can access the interactive Swagger UI at:
http://localhost:8080/swagger-ui.html

This provides:
- Complete list of all available endpoints
- Request/response schemas
- Try-it-out functionality to test endpoints directly
- Authentication support (click "Authorize" and paste your JWT token)

All API endpoints are prefixed with /api. For example:
- /api/users/register - User registration
- /api/users/login - User login
- /api/users/me - Get current user profile
- /api/roles - Role management (admin only)
- /api/admin/stats - Admin statistics (admin only)

Project Structure

rbac/
├── src/
│   ├── main/
│   │   ├── java/com/assignments/rbac/
│   │   │   ├── config/          # Spring configuration classes
│   │   │   ├── controller/      # REST controllers
│   │   │   ├── dto/             # Data transfer objects
│   │   │   ├── entity/          # JPA entities
│   │   │   ├── repository/      # Data repositories
│   │   │   ├── security/        # JWT and security filters
│   │   │   └── service/         # Business logic
│   │   └── resources/
│   │       ├── db/changelog/    # Liquibase migrations
│   │       └── application.properties
│   └── test/                    # Unit and integration tests
├── docker-compose.yml           # Docker Compose configuration
├── Dockerfile                   # Application Docker image
└── pom.xml                      # Maven dependencies

Key Features

Security:
- JWT Authentication: Stateless token-based authentication
- BCrypt Password Hashing: Secure password storage
- Method-Level Security: @PreAuthorize annotations for fine-grained access control
- CSRF Protection: Configured for stateless APIs

Database:
- Liquibase Migrations: Version-controlled database schema changes
- Soft Deletes: Records are marked as deleted rather than physically removed
- Audit Fields: Automatic tracking of creation and modification metadata

Caching:
- User Profile Caching: /api/users/me endpoint is cached for 5 minutes
- Cache Eviction: Cache is automatically cleared when user data changes

Messaging:
- Event Publishing: User registration and login events are published to RabbitMQ
- Decoupled Architecture: Events can be consumed by other services for analytics, notifications, etc.

Troubleshooting

Docker Issues:

Port conflicts: If ports 8080, 3307, 5673, or 15673 are already in use, you can modify the port mappings in docker-compose.yml.

Container won't start: Check the logs with docker-compose logs rbac-app to see what's wrong. Common issues:
- MySQL not ready yet (wait a bit longer)
- Out of memory (Docker Desktop needs at least 4GB allocated)

Database connection errors: Make sure MySQL container is healthy before the app starts. The depends_on with health checks should handle this automatically.

Local Maven Issues:

Database connection refused: Verify MySQL is running and the credentials in application.properties are correct.

Port 8080 already in use: Either stop the other service or change server.port in application.properties.

Tests failing: Some tests require a running database. Make sure MySQL is accessible before running tests.

Development

Running Tests:

To run all tests:
./mvnw test

To run integration tests for RoleController:
./mvnw test -Dtest=RoleControllerIntegrationTest

To run integration tests for AdminController:
./mvnw test -Dtest=AdminControllerIntegrationTest

To run both RoleController and AdminController integration tests:
./mvnw test -Dtest=RoleControllerIntegrationTest,AdminControllerIntegrationTest

The project includes comprehensive test coverage with both unit tests and integration tests. Integration tests use Testcontainers to spin up a real MySQL database and test the full request/response cycle, including security and authentication flows.

Building for Production:
./mvnw clean package -DskipTests

The JAR file will be in target/rbac-0.0.1-SNAPSHOT.jar

Additional Resources

- Postman Collections: Check the postman-collections/ directory for ready-to-use API collections

