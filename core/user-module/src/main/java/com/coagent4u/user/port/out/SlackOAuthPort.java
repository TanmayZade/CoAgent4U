package com.coagent4u.user.port.out;

/**
 * Outbound port — exchanges a Slack OAuth authorization code for user identity.
 * Infrastructure adapters (e.g. SlackOAuthAdapter) implement this.
 *
 * <p>Uses Slack OpenID Connect flow:
 * <ol>
 *   <li>Exchange code via {@code oauth.v2.access}</li>
 *   <li>Retrieve identity via {@code openid.connect.userInfo}</li>
 * </ol>
 * </p>
 *
 * <p>Identification always uses slackUserId + workspaceId (teamId) together.</p>
 */
public interface SlackOAuthPort {

    /**
     * Exchange a Slack OAuth authorization code for user identity.
     *
     * @param code the authorization code from Slack callback
     * @return Slack user identity details
     */
    SlackOAuthResult exchangeCode(String code);

    /**
     * Exchange a Slack OAuth authorization code for bot token and workspace ID
     * (used during workspace installation).
     *
     * @param code the authorization code from Slack callback
     * @return Slack installation result containing the bot token
     */
    SlackInstallationResult exchangeForBotToken(String code);

    /**
     * Value object holding the result of a Slack OAuth exchange.
     * Uses slackUserId + workspaceId (teamId) for unique identification.
     */
    record SlackOAuthResult(
            String slackUserId,
            String workspaceId,
            String workspaceName,
            String workspaceDomain,
            String email,
            String displayName,
            String avatarUrl,
            String accessToken) {
    }

    /**
     * Value object holding the result of a Slack workspace installation exchange.
     */
    record SlackInstallationResult(
            String workspaceId,
            String botToken,
            String installerUserId) {
    }
}
