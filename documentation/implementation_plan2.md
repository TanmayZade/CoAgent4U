# Phase 2 — Corrected Persistence & Infrastructure Plan

## Audit Summary — Violations Found in Current Implementation

| # | Violation | Constraint | Severity |
|---|-----------|------------|----------|
| 1 | V3 `CHECK (state IN (...))` + V7 re-adds CHECK constraint | #3 — State machine duplication | **CRITICAL** |
| 2 | [reconstitute()](file:///e:/CoAgent4U/core/approval-module/src/main/java/com/coagent4u/approval/domain/Approval.java#55-62) added to [User](file:///e:/CoAgent4U/core/user-module/src/main/java/com/coagent4u/user/domain/User.java#31-201), [Agent](file:///e:/CoAgent4U/core/agent-module/src/main/java/com/coagent4u/agent/domain/Agent.java#16-87), [Coordination](file:///e:/CoAgent4U/core/coordination-module/src/main/java/com/coagent4u/coordination/domain/Coordination.java#20-135), [Approval](file:///e:/CoAgent4U/core/approval-module/src/main/java/com/coagent4u/approval/domain/Approval.java#20-134) | Domain layer frozen | **CRITICAL** |
| 3 | [AuditEventHandler](file:///e:/CoAgent4U/infrastructure/monitoring/src/main/java/com/coagent4u/monitoring/AuditEventHandler.java#16-38) calls `UUID.randomUUID()` | #2 — Infra must not generate domain UUIDs | MEDIUM |
| 4 | `@Async` uses unbounded `SimpleAsyncTaskExecutor` | #8 — Bounded executor required | **CRITICAL** |
| 5 | [JwtIssuer](file:///e:/CoAgent4U/infrastructure/security/src/main/java/com/coagent4u/security/JwtIssuer.java#16-37) secret has default fallback in YML | #5 — Fail fast if key missing | HIGH |
| 6 | [AesTokenEncryption](file:///e:/CoAgent4U/infrastructure/security/src/main/java/com/coagent4u/security/AesTokenEncryption.java#15-67) takes raw arg, no boot-time validation | #6 — Env var mandatory, no fallback | HIGH |
| 7 | `SlackSignatureVerifier.verify()` has no timestamp tolerance | #7 — Reject replay | HIGH |
| 8 | Only mock-based [SecurityTests](file:///e:/CoAgent4U/infrastructure/security/src/test/java/com/coagent4u/security/SecurityTests.java#11-107) — no Testcontainers | #9 — Integration tests required | **CRITICAL** |
| 9 | `CoordinationPersistenceAdapter.toJpa()` writes [null](file:///e:/CoAgent4U/shared-kernel/src/test/java/com/coagent4u/shared/ValueObjectsTest.java#82-86) for proposal | #4 — Deterministic JSONB | HIGH |
| 10 | `UserPersistenceAdapter implements UserQueryPort` — port coupling | Hexagonal architecture | MEDIUM |
| 11 | [application.yml](file:///e:/CoAgent4U/coagent-app/src/main/resources/application.yml) hardcodes NeonDB password inline | #10 — No hardcoded secrets | HIGH |
| 12 | No `@EnableConfigurationProperties` for [CoagentProperties](file:///e:/CoAgent4U/infrastructure/config/src/main/java/com/coagent4u/config/CoagentProperties.java#9-229) | #10 — Config binding | LOW |

---

## Corrected Plan

### 1. Migrations — Remove State Machine Duplication

> [!CAUTION]
> **Constraint #3**: The domain [CoordinationState](file:///e:/CoAgent4U/core/coordination-module/src/main/java/com/coagent4u/coordination/domain/CoordinationStateMachine.java#32-79) enum is the single source of truth. A DB CHECK constraint is a fragile duplicate that will **break silently** when a new state is added to the domain enum.

#### [DELETE] [V7__align_schema_with_domain.sql](file:///e:/CoAgent4U/infrastructure/persistence/src/main/resources/db/migration/V7__align_schema_with_domain.sql)

Replace with a corrected V7 that:

#### [NEW] V7__align_schema_with_domain.sql

```sql
-- 1. Users: add missing columns
ALTER TABLE users ADD COLUMN IF NOT EXISTS username VARCHAR(64);
ALTER TABLE users ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;
ALTER TABLE users ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_username
    ON users(username) WHERE username IS NOT NULL;

-- 2. Service connections: add refresh token column
ALTER TABLE service_connections
    ADD COLUMN IF NOT EXISTS encrypted_refresh_token TEXT;

-- 3. Coordinations: DROP the CHECK constraint entirely
--    Domain CoordinationStateMachine is the single source of truth.
--    VARCHAR(32) accepts any value; the domain validates before persisting.
ALTER TABLE coordinations DROP CONSTRAINT IF EXISTS chk_coordination_state;
ALTER TABLE coordinations ADD COLUMN IF NOT EXISTS reason VARCHAR(255);

-- 4. Coordination state log: add reason column
ALTER TABLE coordination_state_log
    ADD COLUMN IF NOT EXISTS reason VARCHAR(255);
```

**Reasoning**: State values are validated by `CoordinationStateMachine.validateTransition()` before any save call. The DB stores whatever the domain produces — it does not enforce state machine rules.

---

### 2. Persistence Adapters — Revert Domain Contamination

> [!IMPORTANT]
> **Domain layer is frozen.** The [reconstitute()](file:///e:/CoAgent4U/core/approval-module/src/main/java/com/coagent4u/approval/domain/Approval.java#55-62) methods added to [User](file:///e:/CoAgent4U/core/user-module/src/main/java/com/coagent4u/user/domain/User.java#31-201), [Agent](file:///e:/CoAgent4U/core/agent-module/src/main/java/com/coagent4u/agent/domain/Agent.java#16-87), [Coordination](file:///e:/CoAgent4U/core/coordination-module/src/main/java/com/coagent4u/coordination/domain/Coordination.java#20-135), [Approval](file:///e:/CoAgent4U/core/approval-module/src/main/java/com/coagent4u/approval/domain/Approval.java#20-134) must be **reverted**. Infrastructure must reconstruct domain objects using existing public APIs only.

#### Reconstitution Strategy (no domain changes)

Each aggregate already has a public constructor or factory. After construction, call [pullDomainEvents()](file:///e:/CoAgent4U/core/user-module/src/main/java/com/coagent4u/user/domain/User.java#156-162) to discard the spurious creation events. The infrastructure mapper owns private reconstitution knowledge — this is an infrastructure concern.

**Pattern** (shown for [User](file:///e:/CoAgent4U/core/user-module/src/main/java/com/coagent4u/user/domain/User.java#31-201)):

```java
public final class UserMapper {
    public static User toDomain(UserJpaEntity e) {
        // 1. Use the existing public factory
        User user = User.register(
            new UserId(e.getUserId()),
            e.getUsername(),
            Email.of(e.getEmail()),
            SlackUserId.of(e.getSlackIdentity().getSlackUserId()),
            WorkspaceId.of(e.getSlackIdentity().getWorkspaceId())
        );
        // 2. Discard the spurious UserRegistered event
        user.pullDomainEvents();

        // 3. Reconnect service connections via public API
        for (ServiceConnectionJpaEntity ce : e.getServiceConnections()) {
            user.connectService(ce.getServiceType(), ce.getEncryptedToken(),
                ce.getEncryptedRefreshToken(), ce.getTokenExpiresAt());
            user.pullDomainEvents(); // discard ServiceConnected events
        }

        // 4. If deleted, call softDelete() and discard event
        if (e.getDeletedAt() != null) {
            user.softDelete();
            user.pullDomainEvents();
        }
        return user;
    }
}
```

> [!WARNING]
> This approach means `createdAt`/`updatedAt` timestamps get regenerated rather than restored from DB. For MVP this is acceptable because the JPA entity is the authoritative snapshot. If precise timestamp restoration becomes necessary post-MVP, the domain can add a [reconstitute()](file:///e:/CoAgent4U/core/approval-module/src/main/java/com/coagent4u/approval/domain/Approval.java#55-62) factory in a future phase — but **not now**.

#### Port Separation

[UserPersistenceAdapter](file:///e:/CoAgent4U/infrastructure/persistence/src/main/java/com/coagent4u/persistence/user/UserPersistenceAdapter.java#14-56) must implement **only** [UserPersistencePort](file:///e:/CoAgent4U/core/user-module/src/main/java/com/coagent4u/user/port/out/UserPersistencePort.java#14-25). The [UserQueryPort](file:///e:/CoAgent4U/core/user-module/src/main/java/com/coagent4u/user/port/out/UserQueryPort.java#14-19) gets a separate `UserQueryAdapter` class. One-adapter-per-port keeps the hexagonal boundaries clean.

#### Files to Create/Fix

| Sub-module | File | Action |
|------------|------|--------|
| `persistence/user/` | [UserMapper.java](file:///e:/CoAgent4U/infrastructure/persistence/src/main/java/com/coagent4u/persistence/user/UserMapper.java) | **REWRITE** — use public API + [pullDomainEvents()](file:///e:/CoAgent4U/core/user-module/src/main/java/com/coagent4u/user/domain/User.java#156-162) |
| `persistence/user/` | [UserPersistenceAdapter.java](file:///e:/CoAgent4U/infrastructure/persistence/src/main/java/com/coagent4u/persistence/user/UserPersistenceAdapter.java) | **REWRITE** — implement [UserPersistencePort](file:///e:/CoAgent4U/core/user-module/src/main/java/com/coagent4u/user/port/out/UserPersistencePort.java#14-25) only |
| `persistence/user/` | `UserQueryAdapter.java` | **NEW** — implement [UserQueryPort](file:///e:/CoAgent4U/core/user-module/src/main/java/com/coagent4u/user/port/out/UserQueryPort.java#14-19) |
| `persistence/agent/` | [AgentMapper.java](file:///e:/CoAgent4U/infrastructure/persistence/src/main/java/com/coagent4u/persistence/agent/AgentMapper.java) | **REWRITE** — use `new Agent(id, userId)` + set state via [deactivate()](file:///e:/CoAgent4U/core/agent-module/src/main/java/com/coagent4u/agent/domain/Agent.java#49-56)/[activate()](file:///e:/CoAgent4U/core/agent-module/src/main/java/com/coagent4u/agent/domain/Agent.java#57-61) + [pullDomainEvents()](file:///e:/CoAgent4U/core/user-module/src/main/java/com/coagent4u/user/domain/User.java#156-162) |
| `persistence/coordination/` | [CoordinationPersistenceAdapter.java](file:///e:/CoAgent4U/infrastructure/persistence/src/main/java/com/coagent4u/persistence/coordination/CoordinationPersistenceAdapter.java) | **REWRITE** — use `new Coordination(…)` + [transition()](file:///e:/CoAgent4U/core/coordination-module/src/main/java/com/coagent4u/coordination/domain/Coordination.java#72-89) loop + [pullDomainEvents()](file:///e:/CoAgent4U/core/user-module/src/main/java/com/coagent4u/user/domain/User.java#156-162), serialize proposal with deterministic ObjectMapper |
| `persistence/coordination/` | `MeetingProposalJsonMapper.java` | **NEW** — explicit `ObjectMapper` for [MeetingProposal](file:///e:/CoAgent4U/core/coordination-module/src/main/java/com/coagent4u/coordination/domain/MeetingProposal.java#12-31) JSONB (no default typing, no polymorphism, explicit field mapping) |
| `persistence/approval/` | [ApprovalPersistenceAdapter.java](file:///e:/CoAgent4U/infrastructure/persistence/src/main/java/com/coagent4u/persistence/approval/ApprovalPersistenceAdapter.java) | **REWRITE** — use `new Approval(…)` + [decide()](file:///e:/CoAgent4U/core/approval-module/src/main/java/com/coagent4u/approval/port/in/DecideApprovalUseCase.java#12-22)/[expire()](file:///e:/CoAgent4U/core/approval-module/src/main/java/com/coagent4u/approval/domain/Approval.java#80-92) + [pullDomainEvents()](file:///e:/CoAgent4U/core/user-module/src/main/java/com/coagent4u/user/domain/User.java#156-162) |
| `persistence/audit/` | [AuditLogJpaEntity.java](file:///e:/CoAgent4U/infrastructure/persistence/src/main/java/com/coagent4u/persistence/audit/AuditLogJpaEntity.java) | **FIX** — remove `DEFAULT NOW()` timestamp; `occurred_at` comes from the domain event's [occurredAt()](file:///e:/CoAgent4U/common-domain/src/main/java/com/coagent4u/common/DomainEvent.java#23-27) field, not from infrastructure |

#### JSONB Serialization (Constraint #4)

```java
public final class MeetingProposalJsonMapper {
    private static final ObjectMapper MAPPER = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        // NO default typing — explicit field mapping only
        .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    public static String toJson(MeetingProposal p) { /* ... */ }
    public static MeetingProposal fromJson(String json) { /* ... */ }
}
```

**Reasoning**: No `@JsonTypeInfo`, no `ObjectMapper.enableDefaultTyping()`. Deterministic, audit-safe, human-readable JSON. The stored document must be interpretable without Java class metadata.

---

### 3. Security — Fail-Fast, No Defaults

#### JWT (Constraint #5)

| Current Issue | Correction |
|---------------|------------|
| `JWT_SECRET` has fallback `dev-jwt-secret-...` | Remove fallback. Inject from [CoagentProperties](file:///e:/CoAgent4U/infrastructure/config/src/main/java/com/coagent4u/config/CoagentProperties.java#9-229). Fail to boot if null/blank. |
| No `jti` claim | Already present ([id(UUID.randomUUID().toString())](file:///e:/CoAgent4U/core/approval-module/src/main/java/com/coagent4u/approval/port/in/DecideApprovalUseCase.java#12-22)) ✅ |
| Expiry not enforced as 24h | Set to `1440` minutes (24h) in config. Validate `> 0` at boot. |
| No PII | Subject = `userId.toString()` only ✅ |

```java
public class JwtIssuer {
    public JwtIssuer(String secret, long expiryMinutes) {
        if (secret == null || secret.isBlank())
            throw new IllegalStateException("JWT signing key must not be blank — set COAGENT_SECURITY_JWT_SECRET");
        if (secret.getBytes(UTF_8).length < 32)
            throw new IllegalStateException("JWT signing key must be >= 32 bytes");
        if (expiryMinutes <= 0)
            throw new IllegalStateException("JWT expiry must be > 0");
        // ...
    }
}
```

#### AES-256-GCM (Constraint #6)

| Current Issue | Correction |
|---------------|------------|
| Key passed as arg, no boot validation | Inject via `CoagentProperties.security.tokenEncryptionKey`. Fail to boot if missing/wrong size. |
| No env var enforcement | Config binds from `${AES_ENCRYPTION_KEY}` — remove `placeholder` fallback |

```java
public AesTokenEncryption(String base64Key) {
    if (base64Key == null || base64Key.isBlank())
        throw new IllegalStateException(
            "AES encryption key must not be blank — set AES_ENCRYPTION_KEY env var");
    byte[] keyBytes = Base64.getDecoder().decode(base64Key);
    if (keyBytes.length != 32)
        throw new IllegalStateException("AES key must be exactly 256 bits (32 bytes)");
    this.key = new SecretKeySpec(keyBytes, "AES");
}
```

#### Slack Signature (Constraint #7)

Add **timestamp tolerance** (5 minutes) and **replay rejection**:

```java
public boolean verify(String timestamp, String body, String signature) {
    // 1. Reject if timestamp older than 5 minutes (replay attack)
    long ts = Long.parseLong(timestamp);
    long now = Instant.now().getEpochSecond();
    if (Math.abs(now - ts) > 300) return false;   // 5-minute window

    // 2. HMAC verification (existing logic)
    // ...
}
```

**Reasoning**: Without a timestamp window, an attacker who intercepts a valid request can replay it indefinitely.

---

### 4. Monitoring & Event Bus — Bounded Executor, Failure Isolation

> [!CAUTION]
> **Constraint #8**: Default `@Async` uses `SimpleAsyncTaskExecutor` which creates an **unbounded number of threads** and can crash the JVM under load.

#### [NEW] `AsyncConfig.java`

```java
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("event-handler-");
        executor.setRejectedExecutionHandler(new CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
```

#### AuditEventHandler Fix (Constraint #2)

The current [AuditEventHandler](file:///e:/CoAgent4U/infrastructure/monitoring/src/main/java/com/coagent4u/monitoring/AuditEventHandler.java#16-38) generates `UUID.randomUUID()` for the audit log ID. This is **acceptable** because [AuditLogJpaEntity](file:///e:/CoAgent4U/infrastructure/persistence/src/main/java/com/coagent4u/persistence/audit/AuditLogJpaEntity.java#11-71) is an infrastructure entity (not a domain aggregate), and the `log_id` is an infrastructure-assigned surrogate key. No domain semantics.

However, the `occurred_at` timestamp **must come from the domain event**, not `NOW()`:

```java
new AuditLogJpaEntity(
    UUID.randomUUID(),          // OK — infrastructure surrogate key
    null,                        // userId extracted per event type
    event.getClass().getSimpleName(),
    null,                        // payload serialized in Phase 3
    null,
    event.occurredAt()           // ✅ domain timestamp, NOT Instant.now()
);
```

#### Failure Isolation

Each `@EventListener @Async` handler must catch all exceptions internally. A failing audit write must **never** propagate and rollback the coordination transaction:

```java
@Async @EventListener
public void handle(DomainEvent event) {
    try {
        auditRepo.save(/* ... */);
    } catch (Exception e) {
        log.error("Audit write failed for event {} — coordination unaffected",
                  event.eventId(), e);
    }
}
```

---

### 5. Config — No Hardcoded Secrets

#### [MODIFY] [application.yml](file:///e:/CoAgent4U/coagent-app/src/main/resources/application.yml)

```yaml
coagent:
  security:
    jwt-secret: ${JWT_SECRET}          # NO fallback — fail to boot if missing
    jwt-expiry-minutes: 1440           # 24 hours
    token-encryption-key: ${AES_ENCRYPTION_KEY}  # NO fallback
  slack:
    signing-secret: ${SLACK_SIGNING_SECRET}      # NO fallback
```

Remove all `placeholder` and `dev-jwt-secret-...` defaults. Environment variables are the only source.

#### [MODIFY] [application-dev.yml](file:///e:/CoAgent4U/coagent-app/src/main/resources/application-dev.yml)

For local development, secrets come from [.env](file:///e:/CoAgent4U/.env) file loaded by Spring's `spring.config.import=optional:file:.env[.properties]` or set in IDE run configuration. **Never committed to VCS**.

#### [NEW] `SecurityBeanConfig.java`

Wire security beans from [CoagentProperties](file:///e:/CoAgent4U/infrastructure/config/src/main/java/com/coagent4u/config/CoagentProperties.java#9-229) with explicit fail-fast validation:

```java
@Configuration
@EnableConfigurationProperties(CoagentProperties.class)
public class SecurityBeanConfig {
    @Bean
    public JwtIssuer jwtIssuer(CoagentProperties props) {
        return new JwtIssuer(
            props.getSecurity().getJwtSecret(),
            props.getSecurity().getJwtExpiryMinutes());
    }
    @Bean
    public JwtValidator jwtValidator(CoagentProperties props) {
        return new JwtValidator(props.getSecurity().getJwtSecret());
    }
    @Bean
    public AesTokenEncryption aesTokenEncryption(CoagentProperties props) {
        return new AesTokenEncryption(props.getSecurity().getTokenEncryptionKey());
    }
    @Bean
    public SlackSignatureVerifier slackSignatureVerifier(CoagentProperties props) {
        return new SlackSignatureVerifier(props.getSlack().getSigningSecret());
    }
    @Bean
    public CaffeineRateLimiter rateLimiter(CoagentProperties props) {
        return new CaffeineRateLimiter(props.getRateLimiting().getRequestsPerMinute());
    }
}
```

---

### 6. Tests — Testcontainers + Failure Isolation

> [!IMPORTANT]
> **Constraint #9**: Mock-only repository tests are **insufficient**. Real PostgreSQL must verify: JSONB behavior, Flyway migrations, `ddl-auto=validate`, transaction rollback, and async isolation.

#### Required Test Classes

| Test Class | Module | Scope |
|------------|--------|-------|
| `UserPersistenceAdapterIT` | persistence | Testcontainers: save/find/delete round-trip, Flyway V1–V7 applied |
| `AgentPersistenceAdapterIT` | persistence | Testcontainers: save/findByUserId |
| `CoordinationPersistenceAdapterIT` | persistence | Testcontainers: JSONB proposal round-trip, state log cascade |
| `ApprovalPersistenceAdapterIT` | persistence | Testcontainers: findPendingByUser query |
| `JwtRoundTripTest` | security | Issue → validate → extract userId ✅ (exists) |
| `JwtFailureTest` | security | Expired token, wrong secret, malformed ✅ (exists) |
| `AesRoundTripTest` | security | Encrypt → decrypt, wrong key fails ✅ (exists) |
| `SlackSignatureReplayTest` | security | **NEW** — verify 5-minute window rejection |
| `AsyncEventIsolationIT` | monitoring | **NEW** — Testcontainers: handler throws → coordination transaction commits |

#### Testcontainers Base Class

```java
@SpringBootTest
@Testcontainers
abstract class PostgresIntegrationTest {
    @Container
    static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void pgProperties(DynamicPropertyRegistry reg) {
        reg.add("spring.datasource.url", pg::getJdbcUrl);
        reg.add("spring.datasource.username", pg::getUsername);
        reg.add("spring.datasource.password", pg::getPassword);
    }
}
```

#### JSONB Round-Trip Test (Constraint #4)

```java
@Test
void proposalJsonRoundTrip() {
    MeetingProposal original = new MeetingProposal(
        "p-1", agentA, agentB, slot, 30, "Standup", "UTC");
    String json = MeetingProposalJsonMapper.toJson(original);
    MeetingProposal restored = MeetingProposalJsonMapper.fromJson(json);
    assertEquals(original, restored);
    // Verify JSON is deterministic
    assertEquals(json, MeetingProposalJsonMapper.toJson(restored));
}
```

#### Async Failure Isolation Test (Constraint #8)

```java
@Test
void auditHandlerFailure_doesNotRollbackCoordination() {
    // 1. Configure AuditEventHandler to throw
    // 2. Save a Coordination + trigger transition (publishes events)
    // 3. Verify: Coordination persisted, AuditLog NOT persisted
    // 4. Verify: no exception propagated to caller
}
```

---

## Files Summary

| Area | File | Action |
|------|------|--------|
| **Migration** | [V7__align_schema_with_domain.sql](file:///e:/CoAgent4U/infrastructure/persistence/src/main/resources/db/migration/V7__align_schema_with_domain.sql) | REWRITE — drop CHECK, no state duplication |
| **Domain** | [User.java](file:///e:/CoAgent4U/core/user-module/src/main/java/com/coagent4u/user/domain/User.java), [Agent.java](file:///e:/CoAgent4U/core/agent-module/src/main/java/com/coagent4u/agent/domain/Agent.java), [Coordination.java](file:///e:/CoAgent4U/core/coordination-module/src/main/java/com/coagent4u/coordination/domain/Coordination.java), [Approval.java](file:///e:/CoAgent4U/core/approval-module/src/main/java/com/coagent4u/approval/domain/Approval.java) | REVERT [reconstitute()](file:///e:/CoAgent4U/core/approval-module/src/main/java/com/coagent4u/approval/domain/Approval.java#55-62) methods |
| **Persistence** | [UserMapper.java](file:///e:/CoAgent4U/infrastructure/persistence/src/main/java/com/coagent4u/persistence/user/UserMapper.java) | REWRITE — public API + [pullDomainEvents()](file:///e:/CoAgent4U/core/user-module/src/main/java/com/coagent4u/user/domain/User.java#156-162) |
| **Persistence** | [UserPersistenceAdapter.java](file:///e:/CoAgent4U/infrastructure/persistence/src/main/java/com/coagent4u/persistence/user/UserPersistenceAdapter.java) | REWRITE — only [UserPersistencePort](file:///e:/CoAgent4U/core/user-module/src/main/java/com/coagent4u/user/port/out/UserPersistencePort.java#14-25) |
| **Persistence** | `UserQueryAdapter.java` | NEW — only [UserQueryPort](file:///e:/CoAgent4U/core/user-module/src/main/java/com/coagent4u/user/port/out/UserQueryPort.java#14-19) |
| **Persistence** | [AgentMapper.java](file:///e:/CoAgent4U/infrastructure/persistence/src/main/java/com/coagent4u/persistence/agent/AgentMapper.java) | REWRITE |
| **Persistence** | [CoordinationPersistenceAdapter.java](file:///e:/CoAgent4U/infrastructure/persistence/src/main/java/com/coagent4u/persistence/coordination/CoordinationPersistenceAdapter.java) | REWRITE — with JSONB mapper |
| **Persistence** | `MeetingProposalJsonMapper.java` | NEW — deterministic ObjectMapper |
| **Persistence** | [ApprovalPersistenceAdapter.java](file:///e:/CoAgent4U/infrastructure/persistence/src/main/java/com/coagent4u/persistence/approval/ApprovalPersistenceAdapter.java) | REWRITE |
| **Security** | [JwtIssuer.java](file:///e:/CoAgent4U/infrastructure/security/src/main/java/com/coagent4u/security/JwtIssuer.java) | FIX — fail-fast validation |
| **Security** | [AesTokenEncryption.java](file:///e:/CoAgent4U/infrastructure/security/src/main/java/com/coagent4u/security/AesTokenEncryption.java) | FIX — fail-fast validation |
| **Security** | [SlackSignatureVerifier.java](file:///e:/CoAgent4U/infrastructure/security/src/main/java/com/coagent4u/security/SlackSignatureVerifier.java) | FIX — 5-min timestamp tolerance |
| **Monitoring** | `AsyncConfig.java` | NEW — bounded ThreadPoolTaskExecutor |
| **Monitoring** | [AuditEventHandler.java](file:///e:/CoAgent4U/infrastructure/monitoring/src/main/java/com/coagent4u/monitoring/AuditEventHandler.java) | FIX — try-catch, use domain timestamp |
| **Monitoring** | [StructuredLogHandler.java](file:///e:/CoAgent4U/infrastructure/monitoring/src/main/java/com/coagent4u/monitoring/StructuredLogHandler.java) | FIX — try-catch |
| **Monitoring** | [MetricsEventHandler.java](file:///e:/CoAgent4U/infrastructure/monitoring/src/main/java/com/coagent4u/monitoring/MetricsEventHandler.java) | FIX — try-catch |
| **Config** | [application.yml](file:///e:/CoAgent4U/coagent-app/src/main/resources/application.yml) | FIX — remove all fallback secrets |
| **Config** | [application-dev.yml](file:///e:/CoAgent4U/coagent-app/src/main/resources/application-dev.yml) | FIX — secrets from [.env](file:///e:/CoAgent4U/.env) only |
| **Config** | `SecurityBeanConfig.java` | NEW — wires security beans |
| **Tests** | 4 Testcontainers ITs + 1 replay test + 1 async isolation IT | NEW |

---

## Architectural Compliance Checklist

| # | Requirement | Status |
|---|-------------|--------|
| 1 | Domain layer unmodified (aggregates, events, state machine, coordination) | ✅ Revert [reconstitute()](file:///e:/CoAgent4U/core/approval-module/src/main/java/com/coagent4u/approval/domain/Approval.java#55-62) |
| 2 | Infrastructure never generates domain UUIDs/timestamps | ✅ Audit `log_id` is infra surrogate key (acceptable); `occurred_at` comes from domain event |
| 3 | No state machine duplication in DB constraints | ✅ DROP CHECK; domain validates before save |
| 4 | JSONB deterministic serialization, no polymorphic typing | ✅ Explicit `MeetingProposalJsonMapper` with `NON_NULL`, `JavaTimeModule`, no default typing |
| 5 | JWT: HS256, userId/iat/exp(24h)/jti, no PII, fail-fast | ✅ Boot-time validation of secret length + expiry |
| 6 | AES-256-GCM: env var only, no fallback, fail-fast | ✅ `IllegalStateException` if key blank/wrong size |
| 7 | Slack HMAC: timestamp tolerance, replay rejection | ✅ 5-minute window + constant-time comparison |
| 8 | Async: bounded executor, handler failure isolation | ✅ `ThreadPoolTaskExecutor(4,8,100)` + try-catch in all handlers |
| 9 | Tests: Testcontainers, JSONB, rollback, async isolation | ✅ 6 integration tests + security round-trips |
| 10 | Config: `@ConfigurationProperties`, no hardcoded secrets, `ddl-auto=validate` | ✅ All secrets from env vars, `SecurityBeanConfig` wires beans |
| 11 | Hexagonal: one adapter per port, no port coupling | ✅ [UserPersistenceAdapter](file:///e:/CoAgent4U/infrastructure/persistence/src/main/java/com/coagent4u/persistence/user/UserPersistenceAdapter.java#14-56) + separate `UserQueryAdapter` |
| 12 | No AI-driven coordination, no approval bypass, no plaintext tokens | ✅ Not introduced |
| 13 | GDPR: soft-delete, encrypted tokens, audit trail | ✅ `deleted_at`, AES-256-GCM, `audit_logs` |

---

## Verification Plan

### Automated
```bash
mvn clean test                    # All 115+ Phase 1 tests still pass
mvn verify -pl coagent4u-persistence,coagent4u-security,coagent4u-monitoring   # Testcontainers ITs
```

### Boot Verification
```bash
# Must fail without secrets:
mvn spring-boot:run -pl coagent-app   # → IllegalStateException: JWT key missing

# Must succeed with secrets:
JWT_SECRET=... AES_ENCRYPTION_KEY=... SLACK_SIGNING_SECRET=... \
  mvn spring-boot:run -pl coagent-app -Dspring-boot.run.profiles=dev
```

### Manual
- `GET /actuator/health` → `UP`
- Flyway history shows V1–V7 applied
- `ddl-auto=validate` passes (schema matches JPA entities)
