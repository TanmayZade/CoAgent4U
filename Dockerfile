# ── Stage 1: Build ──────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build

COPY pom.xml .
COPY shared-kernel/pom.xml shared-kernel/pom.xml
COPY common-domain/pom.xml common-domain/pom.xml
COPY core/user-module/pom.xml core/user-module/pom.xml
COPY core/agent-module/pom.xml core/agent-module/pom.xml
COPY core/coordination-module/pom.xml core/coordination-module/pom.xml
COPY core/approval-module/pom.xml core/approval-module/pom.xml
COPY integration/calendar-module/pom.xml integration/calendar-module/pom.xml
COPY integration/messaging-module/pom.xml integration/messaging-module/pom.xml
COPY integration/llm-module/pom.xml integration/llm-module/pom.xml
COPY infrastructure/persistence/pom.xml infrastructure/persistence/pom.xml
COPY infrastructure/security/pom.xml infrastructure/security/pom.xml
COPY infrastructure/config/pom.xml infrastructure/config/pom.xml
COPY infrastructure/monitoring/pom.xml infrastructure/monitoring/pom.xml
COPY coagent-app/pom.xml coagent-app/pom.xml

# Download dependencies (cached layer)
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -B 2>/dev/null || true

# Copy source and build
COPY . .
RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -DskipTests -B

# ── Stage 2: Runtime ────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S coagent && adduser -S coagent -G coagent

COPY --from=builder /build/coagent-app/target/*.jar app.jar

USER coagent

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
