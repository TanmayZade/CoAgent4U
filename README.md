# CoAgent4U

AI-powered collaborative scheduling platform built as a modular monolith.

## Prerequisites

Before running the project, ensure you have the following installed:
- **Java**: 21 or higher
- **Maven**: 3.9.0 or higher
- **Docker & Docker Compose**: (optional, for running the local PostgreSQL database)

## Environment Setup

1. Copy the `.env.example` file to create a new `.env` file in the root directory:
   ```bash
   cp .env.example .env
   # Or on Windows: copy .env.example .env
   ```
2. Open the newly created `.env` file and fill in your specific configuration values for:
   - Database (Remote NeonDB or Local)
   - Slack Keys
   - Google OAuth credentials
   - Security Secrets (JWT, AES)
   - LLM API Keys (Groq, etc.)

## Database Setup

You have two choices for your PostgreSQL database:

**Option 1: Remote NeonDB (Default)**  
The `.env.example` comes pre-configured with a remote application database URL pointing to NeonDB. Ensure your `.env` is properly populated with the correct NeonDB connection credentials.

**Option 2: Local PostgreSQL Database (Docker Compose)**  
If you prefer not to use the remote NeonDB and want to run your database locally, you can use provided Docker Compose file:
```bash
docker-compose up -d
```
*(Note: If you choose to run locally, remember to update the `DATABASE_URL`, `DATABASE_USERNAME`, and `DATABASE_PASSWORD` inside your `.env` to point to `localhost:5432` with the credentials specified in `docker-compose.yml`.)*

## Building the Project

From the project root directory, run the following command to compile all modules and build the application:

```bash
mvn clean install
```
*(Note: This builds all the core domains, shared kernels, infrastructure modules, and the main application assembly).*

## Running the Application

The `coagent-app` module serves as the primary assembly module containing the Spring Boot application entry point.

**Using Maven:**
```bash
mvn spring-boot:run -pl coagent-app
```

**Using the built executable JAR:**
```bash
java -jar coagent-app/target/coagent-app-0.1.0-SNAPSHOT.jar
```
