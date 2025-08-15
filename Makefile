# Carden Flashcards API - Makefile
# Development and Production Management

.PHONY: help dev prod build clean test

# Default target
help: ## Show this help message
	@echo "🔧 Carden API Management Commands"
	@echo "================================"
	@awk 'BEGIN {FS = ":.*##"} /^[a-zA-Z_-]+:.*##/ { printf "  \033[36m%-20s\033[0m %s\n", $$1, $$2 }' $(MAKEFILE_LIST)

# Development Environment
dev-setup: ## Setup development environment
	@echo "🚀 Setting up development environment..."
	@cp .env.example .env.dev 2>/dev/null || echo "⚠️  .env.dev already exists"
	@docker-compose --env-file .env.dev up -d postgres redis mailhog
	@echo "⏳ Waiting for services..."
	@sleep 10
	@echo "✅ Development environment ready!"
	@echo "📋 Services:"
	@echo "   PostgreSQL: localhost:5432"
	@echo "   Redis: localhost:6379"
	@echo "   Mailhog: http://localhost:8025"

dev-start: ## Start development services (DB, Redis, Mail)
	@echo "📦 Starting development services..."
	@docker-compose --env-file .env.dev up -d postgres redis mailhog

dev-stop: ## Stop development services
	@echo "🛑 Stopping development services..."
	@docker-compose --env-file .env.dev down

dev-reset: ## Reset development database (WARNING: deletes all data)
	@echo "🔄 Resetting development database..."
	@read -p "This will delete ALL data. Continue? (y/N): " confirm && [ "$$confirm" = "y" ]
	@docker-compose --env-file .env.dev down -v
	@docker volume prune -f
	@docker-compose --env-file .env.dev up -d postgres redis mailhog
	@echo "✅ Database reset complete!"

dev-backup: ## Backup development database
	@echo "💾 Creating development database backup..."
	@mkdir -p backups/dev
	@docker-compose --env-file .env.dev exec -T postgres pg_dump -U postgres -d carden_dev > backups/dev/backup_$$(date +%Y%m%d_%H%M%S).sql
	@gzip backups/dev/backup_$$(date +%Y%m%d_%H%M%S).sql 2>/dev/null || true
	@echo "✅ Backup created in backups/dev/"

dev-logs: ## View development logs
	@docker-compose --env-file .env.dev logs -f

# Application Development
app-run: ## Run application in development mode
	@echo "🏃 Starting application..."
	@./gradlew bootRun

app-build: ## Build application JAR
	@echo "🏗️ Building application..."
	@./gradlew build

app-test: ## Run tests
	@echo "🧪 Running tests..."
	@./gradlew test

app-clean: ## Clean build artifacts
	@echo "🧹 Cleaning build artifacts..."
	@./gradlew clean

# Database Management
db-migrate: ## Apply database migrations
	@echo "🗄️ Applying database migrations..."
	@if [ -f .env.dev ]; then export $$(cat .env.dev | grep -v '^#' | xargs); fi && ./gradlew flywayMigrate

db-info: ## Show migration status
	@echo "📊 Database migration status:"
	@if [ -f .env.dev ]; then export $$(cat .env.dev | grep -v '^#' | xargs); fi && ./gradlew flywayInfo

db-clean: ## Clean database (dev only)
	@echo "🧹 Cleaning database..."
	@if [ -f .env.dev ]; then export $$(cat .env.dev | grep -v '^#' | xargs); fi && ./gradlew flywayClean

db-console: ## Open database console
	@echo "💻 Opening database console..."
	@docker-compose --env-file .env.dev exec postgres psql -U postgres -d carden_dev

# Production Environment
prod-setup: ## Setup production environment
	@echo "🚀 Setting up production environment..."
	@if [ ! -f .env.prod ]; then \
		echo "❌ .env.prod not found!"; \
		echo "💡 Copy .env.example to .env.prod and configure it."; \
		exit 1; \
	fi
	@echo "✅ Production environment configured"

prod-build: ## Build production Docker image
	@echo "🏗️ Building production Docker image..."
	@docker-compose -f docker-compose.prod.yml --env-file .env.prod build app

prod-deploy: ## Deploy to production
	@echo "🚀 Deploying to production..."
	@make prod-setup
	@make prod-backup || echo "⚠️  No existing database to backup"
	@docker-compose -f docker-compose.prod.yml --env-file .env.prod up -d --remove-orphans
	@echo "⏳ Waiting for services to start..."
	@sleep 30
	@make prod-health
	@echo "🎉 Production deployment completed!"

prod-start: ## Start production environment
	@echo "▶️  Starting production environment..."
	@docker-compose -f docker-compose.prod.yml --env-file .env.prod up -d

prod-stop: ## Stop production environment
	@echo "⏹️  Stopping production environment..."
	@docker-compose -f docker-compose.prod.yml --env-file .env.prod down

prod-restart: ## Restart production environment
	@echo "🔄 Restarting production environment..."
	@docker-compose -f docker-compose.prod.yml --env-file .env.prod restart

prod-logs: ## View production logs
	@docker-compose -f docker-compose.prod.yml --env-file .env.prod logs -f app

prod-backup: ## Backup production database
	@echo "💾 Creating production database backup..."
	@if [ ! -f .env.prod ]; then echo "❌ .env.prod not found!"; exit 1; fi
	@source .env.prod && \
	mkdir -p backups/prod && \
	docker-compose -f docker-compose.prod.yml --env-file .env.prod exec -T postgres pg_dump \
		-U $$DB_USERNAME -d $$DB_NAME \
		--verbose --no-owner --no-privileges --clean --if-exists \
		> backups/prod/backup_$$(date +%Y%m%d_%H%M%S).sql
	@gzip backups/prod/backup_$$(date +%Y%m%d_%H%M%S).sql 2>/dev/null || true
	@echo "✅ Production backup created"

prod-health: ## Check production health
	@echo "🏥 Checking production health..."
	@echo "📊 Service Status:"
	@docker-compose -f docker-compose.prod.yml --env-file .env.prod ps
	@echo ""
	@echo "🔍 Health Checks:"
	@echo -n "Application: "
	@curl -f http://localhost:8080/api/public/health >/dev/null 2>&1 && echo "✅ Healthy" || echo "❌ Unhealthy"
	@echo -n "Database: "
	@docker-compose -f docker-compose.prod.yml --env-file .env.prod exec -T postgres pg_isready >/dev/null 2>&1 && echo "✅ Connected" || echo "❌ Failed"
	@echo -n "Redis: "
	@docker-compose -f docker-compose.prod.yml --env-file .env.prod exec -T redis redis-cli ping >/dev/null 2>&1 && echo "✅ Connected" || echo "❌ Failed"

# Utility Commands
clean-all: ## Clean all Docker resources
	@echo "🧹 Cleaning all Docker resources..."
	@docker-compose --env-file .env.dev down -v 2>/dev/null || true
	@docker-compose -f docker-compose.prod.yml --env-file .env.prod down -v 2>/dev/null || true
	@docker system prune -f
	@docker volume prune -f

status: ## Show all services status
	@echo "📊 Development Services:"
	@docker-compose --env-file .env.dev ps 2>/dev/null || echo "No development services running"
	@echo ""
	@echo "📊 Production Services:"
	@docker-compose -f docker-compose.prod.yml --env-file .env.prod ps 2>/dev/null || echo "No production services running"

env-example: ## Show environment variable examples
	@echo "📋 Environment template (.env.example):"
	@cat .env.example

# Quick Development Workflow
quick-start: dev-start db-migrate app-run ## Quick start for development (start services + migrate + run app)

quick-reset: dev-reset db-migrate ## Quick reset for development (reset DB + migrate)

# Production Workflow
prod-full-deploy: prod-build prod-deploy ## Full production deployment (build + deploy)

# Security
check-env: ## Check environment configuration
	@echo "🔍 Checking environment configuration..."
	@if [ -f .env.dev ]; then echo "✅ .env.dev exists"; else echo "❌ .env.dev missing"; fi
	@if [ -f .env.prod ]; then echo "✅ .env.prod exists"; else echo "⚠️  .env.prod missing (needed for production)"; fi
