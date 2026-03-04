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
