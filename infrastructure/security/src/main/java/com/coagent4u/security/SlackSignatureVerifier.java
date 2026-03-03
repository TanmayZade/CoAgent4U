package com.coagent4u.security;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Verifies Slack request signatures using HMAC-SHA256.
 * <ul>
 * <li>Enforces 5-minute timestamp tolerance to reject replay attacks</li>
 * <li>Uses constant-time comparison to prevent timing attacks</li>
 * </ul>
 *
 * @see <a href=
 *      "https://api.slack.com/authentication/verifying-requests-from-slack">Slack
 *      Docs</a>
 */
public class SlackSignatureVerifier {

    private static final String VERSION = "v0";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final long MAX_TIMESTAMP_DIFF_SECONDS = 300; // 5 minutes

    private final byte[] signingSecret;

    public SlackSignatureVerifier(String signingSecret) {
        if (signingSecret == null || signingSecret.isBlank()) {
            throw new IllegalStateException(
                    "Slack signing secret must not be blank — set SLACK_SIGNING_SECRET env var");
        }
        this.signingSecret = signingSecret.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Verifies the Slack request signature.
     *
     * @param timestamp the X-Slack-Request-Timestamp header
     * @param body      the raw request body
     * @param signature the X-Slack-Signature header (e.g. "v0=abc123...")
     * @return true if valid and within timestamp tolerance
     */
    public boolean verify(String timestamp, String body, String signature) {
        try {
            // 1. Reject if timestamp is outside the 5-minute tolerance window (replay
            // attack)
            long ts = Long.parseLong(timestamp);
            long now = Instant.now().getEpochSecond();
            if (Math.abs(now - ts) > MAX_TIMESTAMP_DIFF_SECONDS) {
                return false;
            }

            // 2. Compute HMAC-SHA256
            String baseString = VERSION + ":" + timestamp + ":" + body;
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(signingSecret, HMAC_ALGORITHM));
            byte[] hash = mac.doFinal(baseString.getBytes(StandardCharsets.UTF_8));
            String computed = VERSION + "=" + bytesToHex(hash);

            // 3. Constant-time comparison
            return MessageDigest.isEqual(
                    computed.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("HMAC computation failed", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
