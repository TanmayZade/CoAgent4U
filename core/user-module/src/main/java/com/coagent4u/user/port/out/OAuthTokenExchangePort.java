package com.coagent4u.user.port.out;

import java.time.Instant;

/**
 * Outbound port — exchanges an OAuth authorization code for token pair.
 * Infrastructure adapters (e.g. GoogleCalendarAdapter) implement this.
 *
 * <p>
 * The returned tokens are already encrypted (AES-256-GCM) and safe for
 * direct persistence.
 * </p>
 */
public interface OAuthTokenExchangePort {

    /**
     * Exchange an OAuth authorization code for an encrypted token pair.
     *
     * @param authorizationCode the code from the OAuth callback
     * @return the exchange result containing encrypted tokens
     */
    OAuthTokenResult exchangeCode(String authorizationCode);

    /**
     * Value object holding the result of an OAuth token exchange.
     * All token fields are already AES-256-GCM encrypted.
     */
    record OAuthTokenResult(
            String encryptedAccessToken,
            String encryptedRefreshToken,
            Instant expiresAt) {
    }
}
