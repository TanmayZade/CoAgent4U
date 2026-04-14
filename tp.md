# Start the app
mvn spring-boot:run -pl coagent-app

# Health check
curl http://localhost:8080/api/health
# → "OK"

# Register a user
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","email":"alice@example.com","slackUserId":"U123","workspaceId":"T123"}'

# Get user profile (use the UUID from DB)
curl http://localhost:8080/api/users/299c47ed-1fe0-4fbe-b84b-f23f2d167cc0

# OAuth callback (stub — returns Phase 4 message)
curl "http://localhost:8080/api/oauth2/callback?code=test_code"

mvn install -DskipTests -pl infrastructure/persistence -am && mvn spring-boot:run -pl coagent-app


# 1. Test Tier 1 Regex (Positive Match)
$body = @{ text = "Schedule a dentist appointment tomorrow at 2pm" } | ConvertTo-Json
Invoke-RestMethod -Uri http://localhost:8080/api/sandbox/parse-intent -Method POST -Body $body -ContentType "application/json"
# 2. Test Collaboration Match
$body = @{ text = "Set up a meeting with @alice" } | ConvertTo-Json
Invoke-RestMethod -Uri http://localhost:8080/api/sandbox/parse-intent -Method POST -Body $body -ContentType "application/json"
# 3. Test Unknown (Tier 2 Fallback)
# This will show 'SKIPPED' for Tier 1 and attempt LLM (returning 'FAILED_OR_DISABLED' unless you add a Groq API key)
$body = @{ text = "I am bored, tell me a joke" } | ConvertTo-Json
Invoke-RestMethod -Uri http://localhost:8080/api/sandbox/parse-intent -Method POST -Body $body -ContentType "application/json"


https://github.com/v-3/google-calendar

https://github.com/nspady/google-calendar-mcp
