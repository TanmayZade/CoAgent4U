package com.coagent4u.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.coagent4u.common.exception.ExternalServiceUnavailableException;
import com.coagent4u.config.CoagentProperties;
import com.coagent4u.user.port.out.SlackOAuthPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Slack OAuth adapter using OpenID Connect flow.
 * <ol>
 *   <li>Exchanges authorization code via {@code oauth.v2.access}</li>
 *   <li>Retrieves user identity via {@code openid.connect.userInfo}</li>
 * </ol>
 *
 * <p>Uses slackUserId + workspaceId (teamId) for unique identification.</p>
 */
@Component
public class SlackOAuthAdapter implements SlackOAuthPort {

    private static final Logger log = LoggerFactory.getLogger(SlackOAuthAdapter.class);
    private static final String SLACK_OAUTH_ACCESS_URL = "https://slack.com/api/oauth.v2.access";
    private static final String SLACK_OPENID_USERINFO_URL = "https://slack.com/api/openid.connect.userInfo";

    private final CoagentProperties properties;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public SlackOAuthAdapter(CoagentProperties properties,
                             WebClient.Builder webClientBuilder,
                             ObjectMapper objectMapper) {
        this.properties = properties;
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    @Override
    public SlackOAuthResult exchangeCode(String code) {
        log.info("Exchanging Slack OAuth authorization code");

        // Step 1: Exchange code for access token
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("code", code);
        formData.add("client_id", properties.getSlack().getClientId());
        formData.add("client_secret", properties.getSlack().getClientSecret());
        formData.add("redirect_uri", properties.getSlack().getRedirectUri());

        try {
            String tokenResponse = webClient.post()
                    .uri(SLACK_OAUTH_ACCESS_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(formData))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode tokenJson = objectMapper.readTree(tokenResponse);

            if (!tokenJson.path("ok").asBoolean(false)) {
                String error = tokenJson.path("error").asText("unknown_error");
                log.warn("Slack OAuth token exchange failed: {}", error);
                throw new ExternalServiceUnavailableException("SlackOAuth",
                        "Token exchange failed: " + error);
            }

            // Extract authed_user info and access token
            JsonNode authedUser = tokenJson.path("authed_user");
            String slackUserId = authedUser.path("id").asText();
            String accessToken = authedUser.path("access_token").asText();
            String workspaceId = tokenJson.path("team").path("id").asText();

            if (slackUserId.isBlank() || workspaceId.isBlank()) {
                throw new ExternalServiceUnavailableException("SlackOAuth",
                        "Missing slackUserId or workspaceId in token response");
            }

            // Step 2: Get user identity via OpenID Connect
            String userInfoResponse = webClient.get()
                    .uri(SLACK_OPENID_USERINFO_URL)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode userInfoJson = objectMapper.readTree(userInfoResponse);

            if (!userInfoJson.path("ok").asBoolean(false)) {
                String error = userInfoJson.path("error").asText("unknown_error");
                log.warn("Slack OpenID userInfo failed: {}", error);
                throw new ExternalServiceUnavailableException("SlackOAuth",
                        "OpenID Connect userInfo failed: " + error);
            }

            String email = userInfoJson.path("email").asText(null);
            String displayName = userInfoJson.path("name").asText(
                    userInfoJson.path("given_name").asText("user"));
            String avatarUrl = userInfoJson.path("picture").asText(null);

            // Workspace metadata is prefixed with https://slack.com/ in OIDC flow
            String workspaceName = userInfoJson.path("https://slack.com/team_name").asText("Slack Workspace");
            String workspaceDomain = userInfoJson.path("https://slack.com/team_domain").asText(null);

            log.info("Slack OAuth successful: slackUserId={}, workspaceId={}, workspaceName={}, workspaceDomain={}, avatarUrl={}", 
                    slackUserId, workspaceId, workspaceName, workspaceDomain, avatarUrl);

            return new SlackOAuthResult(slackUserId, workspaceId, workspaceName, workspaceDomain, email, displayName, avatarUrl, accessToken);

        } catch (WebClientResponseException e) {
            log.warn("Slack OAuth HTTP error: {} — {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ExternalServiceUnavailableException("SlackOAuth",
                    "HTTP " + e.getStatusCode(), e);
        } catch (ExternalServiceUnavailableException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Slack OAuth exchange failed: {}", e.getMessage());
            throw new ExternalServiceUnavailableException("SlackOAuth",
                    "Exchange failed: " + e.getMessage(), e);
        }
    }

    @Override
    public SlackInstallationResult exchangeForBotToken(String code) {
        log.info("Exchanging Slack OAuth authorization code for Bot Token installation");

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("code", code);
        formData.add("client_id", properties.getSlack().getClientId());
        formData.add("client_secret", properties.getSlack().getClientSecret());
        formData.add("redirect_uri", properties.getSlack().getRedirectUri());

        try {
            String tokenResponse = webClient.post()
                    .uri(SLACK_OAUTH_ACCESS_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(formData))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode tokenJson = objectMapper.readTree(tokenResponse);

            if (!tokenJson.path("ok").asBoolean(false)) {
                String error = tokenJson.path("error").asText("unknown_error");
                log.warn("Slack Bot Token exchange failed: {}", error);
                throw new ExternalServiceUnavailableException("SlackOAuth",
                        "Token exchange failed: " + error);
            }

            JsonNode authedUser = tokenJson.path("authed_user");
            String installerUserId = authedUser.path("id").asText();
            String botToken = tokenJson.path("access_token").asText();
            String workspaceId = tokenJson.path("team").path("id").asText();

            if (botToken.isBlank() || workspaceId.isBlank()) {
                throw new ExternalServiceUnavailableException("SlackOAuth",
                        "Missing bot_token or workspaceId in token response");
            }

            log.info("Slack Bot Token installation successful: workspaceId={}, installerUserId={}", 
                    workspaceId, installerUserId);

            return new SlackInstallationResult(workspaceId, botToken, installerUserId);

        } catch (WebClientResponseException e) {
            log.warn("Slack OAuth HTTP error: {} — {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ExternalServiceUnavailableException("SlackOAuth",
                    "HTTP " + e.getStatusCode(), e);
        } catch (ExternalServiceUnavailableException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Slack OAuth Bot Token exchange failed: {}", e.getMessage());
            throw new ExternalServiceUnavailableException("SlackOAuth",
                    "Exchange failed: " + e.getMessage(), e);
        }
    }
}
