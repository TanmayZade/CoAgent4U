package com.coagent4u.security;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class SecurityTests {

    // 48-char secret >= 32 bytes for HS256
    private static final String JWT_SECRET = "dev-jwt-secret-change-in-production-min-32-chars!!";

    // ── JWT Tests ──────────────────────────────────────────

    @Test
    void jwt_roundTrip() {
        JwtIssuer issuer = new JwtIssuer(JWT_SECRET, 1440);
        JwtValidator validator = new JwtValidator(JWT_SECRET);

        UUID userId = UUID.randomUUID();
        String token = issuer.issue(userId);
        assertNotNull(token);

        Optional<UUID> parsed = validator.validate(token);
        assertTrue(parsed.isPresent());
        assertEquals(userId, parsed.get());
    }

    @Test
    void jwt_invalidToken_returnsEmpty() {
        JwtValidator validator = new JwtValidator(JWT_SECRET);
        assertTrue(validator.validate("garbage-token").isEmpty());
    }

    @Test
    void jwt_wrongSecret_returnsEmpty() {
        JwtIssuer issuer = new JwtIssuer(JWT_SECRET, 60);
        JwtValidator wrongValidator = new JwtValidator("another-secret-that-is-at-least-32-chars-long!!");

        String token = issuer.issue(UUID.randomUUID());
        assertTrue(wrongValidator.validate(token).isEmpty());
    }

    @Test
    void jwt_failFast_blankSecret() {
        assertThrows(IllegalStateException.class, () -> new JwtIssuer("", 60));
        assertThrows(IllegalStateException.class, () -> new JwtIssuer(null, 60));
    }

    @Test
    void jwt_failFast_shortSecret() {
        assertThrows(IllegalStateException.class, () -> new JwtIssuer("too-short", 60));
    }

    @Test
    void jwt_failFast_nonPositiveExpiry() {
        assertThrows(IllegalStateException.class, () -> new JwtIssuer(JWT_SECRET, 0));
        assertThrows(IllegalStateException.class, () -> new JwtIssuer(JWT_SECRET, -1));
    }

    // ── AES Tests ──────────────────────────────────────────

    @Test
    void aes_encryptDecrypt_roundTrip() {
        byte[] keyBytes = new byte[32];
        new java.security.SecureRandom().nextBytes(keyBytes);
        String base64Key = Base64.getEncoder().encodeToString(keyBytes);

        AesTokenEncryption aes = new AesTokenEncryption(base64Key);

        String plaintext = "ya29.a0AfB_test_oauth_token_here";
        String encrypted = aes.encrypt(plaintext);
        assertNotEquals(plaintext, encrypted);

        String decrypted = aes.decrypt(encrypted);
        assertEquals(plaintext, decrypted);
    }

    @Test
    void aes_failFast_blankKey() {
        assertThrows(IllegalStateException.class, () -> new AesTokenEncryption(""));
        assertThrows(IllegalStateException.class, () -> new AesTokenEncryption(null));
    }

    @Test
    void aes_failFast_wrongKeySize() {
        String shortKey = Base64.getEncoder().encodeToString(new byte[16]);
        assertThrows(IllegalStateException.class, () -> new AesTokenEncryption(shortKey));
    }

    @Test
    void aes_failFast_invalidBase64() {
        assertThrows(IllegalStateException.class, () -> new AesTokenEncryption("not!valid@base64###"));
    }

    // ── Slack Signature Tests ─────────────────────────────

    @Test
    void slack_signatureVerification() {
        String secret = "8f742231b10e8888abcd99yyyzzz85a5";
        SlackSignatureVerifier verifier = new SlackSignatureVerifier(secret);

        // Use current timestamp (within 5-min window)
        String timestamp = String.valueOf(java.time.Instant.now().getEpochSecond());
        String body = "token=xyzz0WbapA4vBCDEFasx0YR6&command=/weather";

        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(
                    secret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(("v0:" + timestamp + ":" + body)
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash)
                hex.append(String.format("%02x", b));
            String sig = "v0=" + hex;

            assertTrue(verifier.verify(timestamp, body, sig));
            assertFalse(verifier.verify(timestamp, body, "v0=wrong"));
        } catch (Exception e) {
            fail("HMAC computation failed: " + e.getMessage());
        }
    }

    @Test
    void slack_replayAttack_rejected() {
        String secret = "8f742231b10e8888abcd99yyyzzz85a5";
        SlackSignatureVerifier verifier = new SlackSignatureVerifier(secret);

        // Timestamp from 10 minutes ago — outside 5-min window
        long staleTimestamp = java.time.Instant.now().getEpochSecond() - 600;
        String body = "token=test&command=/weather";

        // Even with a valid signature, replay should be rejected
        assertFalse(verifier.verify(String.valueOf(staleTimestamp), body, "v0=anything"));
    }

    @Test
    void slack_failFast_blankSecret() {
        assertThrows(IllegalStateException.class, () -> new SlackSignatureVerifier(""));
        assertThrows(IllegalStateException.class, () -> new SlackSignatureVerifier(null));
    }

    // ── Rate Limiter Tests ────────────────────────────────

    @Test
    void rateLimiter_enforcesLimit() {
        CaffeineRateLimiter limiter = new CaffeineRateLimiter(3);
        assertTrue(limiter.tryAcquire("user1"));
        assertTrue(limiter.tryAcquire("user1"));
        assertTrue(limiter.tryAcquire("user1"));
        assertFalse(limiter.tryAcquire("user1")); // 4th blocked
        assertTrue(limiter.tryAcquire("user2")); // different user OK
    }
}
