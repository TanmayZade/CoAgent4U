# Phase 0 — Project Bootstrap Implementation Plan

Build system works end-to-end with zero business logic. All modules compile, Docker Compose starts PostgreSQL, Flyway creates the schema, and `/actuator/health` returns `UP`.

---

## Proposed Changes

### Root Build Configuration

#### [NEW] [pom.xml](file:///e:/CoAgent4U/pom.xml)

Root Maven POM with reactor build listing all 14 sub-modules. Sets:
- Java 21
- Spring Boot 3.4.x BOM (via `spring-boot-starter-parent`)
- Maven Enforcer Plugin (require Java 21, Maven 3.9+)
- Common properties: `project.build.sourceEncoding=UTF-8`

Modules declared:
```
shared-kernel, common-domain,
core/user-module, core/agent-module, core/coordination-module, core/approval-module,
integration/calendar-module, integration/messaging-module, integration/llm-module,
infrastructure/persistence, infrastructure/security, infrastructure/config, infrastructure/monitoring,
coagent-app
```

---

### shared-kernel (Step 0.2)

#### [NEW] [shared-kernel/pom.xml](file:///e:/CoAgent4U/shared-kernel/pom.xml)
Zero external dependencies. Pure Java module.

#### [NEW] [UserId.java](file:///e:/CoAgent4U/shared-kernel/src/main/java/com/coagent4u/shared/UserId.java)
Placeholder `record UserId(UUID value)` with null-check in compact constructor.

#### [NEW] [package-info.java](file:///e:/CoAgent4U/shared-kernel/src/main/java/com/coagent4u/shared/package-info.java)

---

### common-domain (Step 0.3)

#### [NEW] [common-domain/pom.xml](file:///e:/CoAgent4U/common-domain/pom.xml)
Depends only on `shared-kernel`. Zero Spring dependencies.

#### [NEW] [DomainEvent.java](file:///e:/CoAgent4U/common-domain/src/main/java/com/coagent4u/common/DomainEvent.java)
Marker interface (empty).

#### [NEW] [DomainEventPublisher.java](file:///e:/CoAgent4U/common-domain/src/main/java/com/coagent4u/common/DomainEventPublisher.java)
Port interface: `void publish(DomainEvent event)`.

#### [NEW] [package-info.java](file:///e:/CoAgent4U/common-domain/src/main/java/com/coagent4u/common/package-info.java)

---

### Skeleton Modules (Step 0.4)

Each module gets a `pom.xml` with appropriate dependencies and a `package-info.java`:

**Core modules** (depend on `shared-kernel` + `common-domain`):
- [NEW] `core/user-module/` — package `com.coagent4u.user`
- [NEW] `core/agent-module/` — package `com.coagent4u.agent`
- [NEW] `core/coordination-module/` — package `com.coagent4u.coordination`
- [NEW] `core/approval-module/` — package `com.coagent4u.approval`

**Integration modules** (depend on `shared-kernel`):
- [NEW] `integration/calendar-module/` — package `com.coagent4u.calendar`
- [NEW] `integration/messaging-module/` — package `com.coagent4u.messaging`
- [NEW] `integration/llm-module/` — package `com.coagent4u.llm`

**Infrastructure modules**:
- [NEW] `infrastructure/persistence/` — package `com.coagent4u.persistence`
- [NEW] `infrastructure/security/` — package `com.coagent4u.security`
- [NEW] `infrastructure/config/` — package `com.coagent4u.config`
- [NEW] `infrastructure/monitoring/` — package `com.coagent4u.monitoring`

---

### Docker Compose (Step 0.5)

#### [NEW] [docker-compose.yml](file:///e:/CoAgent4U/docker-compose.yml)
PostgreSQL 15, port 5432, health check with `pg_isready`, volume for data persistence. App placeholder service (commented out until Step 0.6 is complete).

---

### coagent-app (Step 0.6)

#### [NEW] [coagent-app/pom.xml](file:///e:/CoAgent4U/coagent-app/pom.xml)
Depends on ALL modules. Includes `spring-boot-starter-web`, `spring-boot-starter-actuator`, `spring-boot-starter-data-jpa`, Flyway, PostgreSQL driver.

#### [NEW] [CoAgent4UApplication.java](file:///e:/CoAgent4U/coagent-app/src/main/java/com/coagent4u/CoAgent4UApplication.java)
`@SpringBootApplication(scanBasePackages = "com.coagent4u")`

#### [NEW] [application.yml](file:///e:/CoAgent4U/coagent-app/src/main/resources/application.yml)
Base config with `spring.datasource`, `spring.jpa.hibernate.ddl-auto=validate`, `spring.flyway.enabled=true`, actuator health exposure.

#### [NEW] [application-dev.yml](file:///e:/CoAgent4U/coagent-app/src/main/resources/application-dev.yml)
Dev profile with `ddl-auto=validate`, debug logging, datasource pointing to `${DATABASE_URL}`.

---

### Flyway Migration (Step 0.7)

#### [NEW] [V1__init_schema.sql](file:///e:/CoAgent4U/coagent-app/src/main/resources/db/migration/V1__init_schema.sql)

Single initial migration with all tables from PRD §9.1:
- `users` — with username regex check, soft delete
- `slack_identities` — composite unique (team_id, slack_user_id)
- `service_connections` — encrypted OAuth tokens
- `agents` — FK to users
- `coordinations` — no cross-module FKs, JSONB proposal
- `coordination_state_log` — FK to coordinations
- `approvals` — no cross-module FKs
- `audit_logs` — append-only

All indexes as specified in PRD §9.1.

---

## Verification Plan

### Automated Tests

1. **Maven compilation**: `mvn clean compile` from project root — must succeed with zero errors
2. **Maven package**: `mvn clean package -DskipTests` — must produce `coagent-app/target/*.jar`
3. **Docker PostgreSQL**: `docker-compose up -d coagent-db` then `docker exec coagent-db pg_isready -U postgres` — returns success
4. **Spring Boot startup + Flyway + Health**: Start with `mvn spring-boot:run -pl coagent-app -Dspring-boot.run.profiles=dev` — verify:
   - Flyway runs migration successfully (log shows `V1__init_schema.sql`)
   - `/actuator/health` returns `{"status":"UP"}`
5. **Schema verification**: Connect to PostgreSQL and verify all 8 tables exist:
   ```sql
   SELECT table_name FROM information_schema.tables WHERE table_schema = 'public';
   ```

# CoAgent4U — Phase 0: Complete ✅

> **Health check confirmed:** `{"status":"UP"}` — PostgreSQL UP on NeonDB 17.8

---

## Delivery Summary

| Step | Module / Artifact | Description | Status |
|------|-------------------|-------------|--------|
| 0.1 | `pom.xml` (root) | Java 21, Spring Boot 3.4.3, Maven Enforcer, 14 modules | ✅ |
| 0.2 | `shared-kernel` | `UserId` record | ✅ |
| 0.3 | `common-domain` | `DomainEvent` + `DomainEventPublisher` | ✅ |
| 0.4 | 11 skeleton modules | core × 4, integration × 3, infrastructure × 4 | ✅ |
| 0.5 | `docker-compose.yml` | PostgreSQL 15 | ✅ |
| 0.6 | `coagent-app` | Spring Boot app, `application.yml`, `application-dev.yml` | ✅ |
| 0.7 | `V1__init_schema.sql` | All 8 tables, all indexes | ✅ |

---

## Exit Criteria

All roadmap exit criteria met:

- `mvn clean compile` → **BUILD SUCCESS** (15/15 modules)
- Spring Boot starts on **port 8080** with `profile: dev`
- Flyway applied `V1__init_schema.sql` to NeonDB PostgreSQL 17.8 — *"1 migration applied, now at version v1"*
- `/actuator/health` → `{"status":"UP"}` with `db` component showing **PostgreSQL UP**

> **Note:** The favicon 404 is expected — no static assets have been added yet.

---

## Health Check Response

```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 53160603648,
        "free": 29817933824,
        "threshold": 10485760,
        "path": "E:\\CoAgent4U\\.",
        "exists": true
      }
    },
    "ping": { "status": "UP" },
    "ssl": {
      "status": "UP",
      "details": {
        "validChains": [],
        "invalidChains": []
      }
    }
  }
}
```

---

## Up Next — Phase 1: User Module

The real domain logic begins here:

- `User` aggregate
- `RegisterUserUseCase`
- `SlackIdentity`
- Supporting repository & persistence layer