# Carden Flashcards API

A modern flashcard application backend built with Spring Boot 3, PostgreSQL, and Redis.

## 🚀 Quick Start

### Prerequisites

- Java 21+
- Docker & Docker Compose
- Git

### 1. Clone and Setup

```bash
git clone <repository-url>
cd carden
cp .env.example .env.dev  # Copy and edit development environment variables
```

### 2. Start Development Environment

```bash
# Quick start (recommended)
make quick-start

# Or step by step
make dev-setup           # Setup development environment
make db-migrate          # Apply database migrations  
make app-run            # Start the application
```

### 3. Access Services

- **API**: http://localhost:8080/api
- **Swagger UI**: http://localhost:8080/api/swagger-ui.html
- **Health Check**: http://localhost:8080/api/public/health
- **Mailhog** (Email testing): http://localhost:8025

## 🗄️ Development Management

### Available Make Commands

```bash
# Quick commands
make help               # Show all available commands
make quick-start        # Start services + migrate + run app
make quick-reset        # Reset DB + migrate

# Development environment
make dev-setup          # Setup development environment
make dev-start          # Start services (DB, Redis, Mail)
make dev-stop           # Stop services
make dev-reset          # Reset database (deletes all data)
make dev-backup         # Backup development database
make dev-logs           # View service logs

# Application development
make app-run            # Run application
make app-build          # Build JAR
make app-test           # Run tests
make app-clean          # Clean build artifacts

# Database management
make db-migrate         # Apply migrations
make db-info            # Show migration status
make db-clean           # Clean database (dev only)
make db-console         # Open database console
```

### Database Connection

```
Host: localhost
Port: 5432
Database: carden_dev
Username: postgres
Password: password
```

## 🔧 Configuration

### Environment Files

- `.env.example` - Environment template (tracked in git)
- `.env.dev` - Development environment variables (create from .env.example)
- `.env.prod` - Production environment variables (create from .env.example)

### Application Profiles

- **dev**: Development environment with debugging enabled
- **prod**: Production environment with optimized settings

### Key Environment Variables

**Development (.env.dev):**
```bash
# Database
DB_HOST=localhost
DB_USERNAME=postgres
DB_PASSWORD=password

# Redis
REDIS_HOST=localhost
REDIS_PASSWORD=redispassword

# JWT (Development - longer expiration)
JWT_SECRET=development-secret-key

# Email (Mailhog for dev)
MAIL_HOST=localhost
MAIL_PORT=1025
```

**Production (.env.prod):**
```bash
# Database
DB_USERNAME=carden_user
DB_PASSWORD=strong_production_password

# Redis
REDIS_PASSWORD=strong_redis_password

# JWT (Production - secure secret)
JWT_SECRET=very_long_and_secure_production_secret

# Email (Production SMTP)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-app@yourdomain.com
```

## 📊 Database Schema

The application uses Flyway for database migrations. Migration files are located in `src/main/resources/db/migration/`.

### Current Tables

- `users` - User accounts and authentication
- `decks` - Flashcard decks
- `cards` - Individual flashcards
- `topics` - Subject categories
- `study_states` - User progress tracking
- `review_sessions` - Study session records
- `reports` - Analytics and reporting

## 🔐 Authentication

The API uses JWT-based authentication with the following endpoints:

- `POST /api/v1/auth/register` - User registration
- `POST /api/v1/auth/login` - User login
- `POST /api/v1/auth/logout` - User logout
- `GET /api/v1/auth/me` - Get current user profile

## 🧪 Testing

```bash
# Run tests
./gradlew test

# Run tests with coverage
./gradlew test jacocoTestReport
```

## 📝 API Documentation

Interactive API documentation is available at:
- **Swagger UI**: http://localhost:8080/api/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/api/v3/api-docs

## 🔧 Development Tips

### Development Tips

```bash
# Reset everything (clean slate)
make quick-reset

# Check service status
make status

# Database console access
make db-console

# View logs
make dev-logs
```

## 🚀 Production Deployment

### Production Commands

```bash
# Production setup
make prod-setup         # Verify .env.prod configuration
make prod-build         # Build production Docker image
make prod-deploy        # Full deployment with backup
make prod-start         # Start production services
make prod-stop          # Stop production services
make prod-restart       # Restart production services

# Production monitoring
make prod-health        # Check all services health
make prod-logs          # View application logs
make prod-backup        # Backup production database

# Quick production deployment
make prod-full-deploy   # Build + Deploy in one command
```

### Production Environment Setup

1. **Copy production environment file:**
   ```bash
   cp .env.example .env.prod
   # Edit .env.prod with your production values
   ```

2. **Configure production settings:**
   - Strong database passwords
   - Secure JWT secret (256+ bits)
   - Production SMTP settings
   - SSL certificates (if using HTTPS)

3. **Deploy:**
   ```bash
   make prod-full-deploy
   ```

### Production Architecture

- **Backend**: Runs in Docker container with optimized JVM settings
- **Database**: PostgreSQL with production tuning
- **Cache**: Redis with persistence and memory optimization
- **Proxy**: Nginx with rate limiting and SSL (optional)
- **Monitoring**: Health checks and resource monitoring

## 📁 Project Structure

```
src/
├── main/
│   ├── java/com/loopy/carden/
│   │   ├── config/          # Configuration classes
│   │   ├── controller/      # REST controllers
│   │   ├── dto/            # Data transfer objects
│   │   ├── entity/         # JPA entities
│   │   ├── exception/      # Exception handling
│   │   ├── repository/     # Data access layer
│   │   ├── security/       # Security configuration
│   │   └── service/        # Business logic
│   └── resources/
│       ├── application*.yml # Configuration files
│       └── db/migration/   # Database migrations
├── scripts/
│   └── database/          # Database management scripts
└── docker-compose.yml    # Development services
```
