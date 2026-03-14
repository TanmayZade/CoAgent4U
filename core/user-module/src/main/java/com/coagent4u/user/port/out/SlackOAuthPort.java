package com.coagent4u.user.port.out;

/**
 * Outbound port — exchanges a Slack OAuth authorization code for user identity.
 * Infrastructure adapters (e.g. SlackOAuthAdapter) implement this.
 *
 * <p>
 * Uses Slack OpenID Connect flow:
 * <ol>
 * <li>Exchange code via {@code oauth.v2.access}</li>
 * <li>Retrieve identity via {@code openid.connect.userInfo}</li>
 * </ol>
 * </p>
 *
 * <p>
 * Identification always uses slackUserId + workspaceId (teamId) together.
 * </p>
 */
public interface SlackOAuthPort {

        SlackOAuthResult exchangeCode(String code);

        /**
         * Exchange a Slack OAuth authorization code for a bot token.
         * Used for app installation to support multiple workspaces.
         *
         * @param code the authorization code from Slack callback
         * @return Slack installation details with bot token
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
         * Value object holding the result of a Slack App installation.
         * Contains the bot access token for a specific workspace.
         */
        record SlackInstallationResult(
                        String workspaceId,
                        String botToken,
                        String installerUserId) {
        }
}
