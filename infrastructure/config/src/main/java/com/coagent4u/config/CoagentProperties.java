package com.coagent4u.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed configuration for all CoAgent4U custom properties.
 * Bound from application.yml under the "coagent" prefix.
 */
@ConfigurationProperties(prefix = "coagent")
public class CoagentProperties {

    private Slack slack = new Slack();
    private Google google = new Google();
    private Security security = new Security();
    private Coordination coordination = new Coordination();
    private Llm llm = new Llm();
    private RateLimiting rateLimiting = new RateLimiting();

    public Slack getSlack() {
        return slack;
    }

    public void setSlack(Slack slack) {
        this.slack = slack;
    }

    public Google getGoogle() {
        return google;
    }

    public void setGoogle(Google google) {
        this.google = google;
    }

    public Security getSecurity() {
        return security;
    }

    public void setSecurity(Security security) {
        this.security = security;
    }

    public Coordination getCoordination() {
        return coordination;
    }

    public void setCoordination(Coordination coordination) {
        this.coordination = coordination;
    }

    public Llm getLlm() {
        return llm;
    }

    public void setLlm(Llm llm) {
        this.llm = llm;
    }

    public RateLimiting getRateLimiting() {
        return rateLimiting;
    }

    public void setRateLimiting(RateLimiting rateLimiting) {
        this.rateLimiting = rateLimiting;
    }

    public static class Slack {
        private String signingSecret;
        private String botToken;
        private String appId;

        public String getSigningSecret() {
            return signingSecret;
        }

        public void setSigningSecret(String s) {
            this.signingSecret = s;
        }

        public String getBotToken() {
            return botToken;
        }

        public void setBotToken(String s) {
            this.botToken = s;
        }

        public String getAppId() {
            return appId;
        }

        public void setAppId(String s) {
            this.appId = s;
        }
    }

    public static class Google {
        private String clientId;
        private String clientSecret;
        private String redirectUri;

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String s) {
            this.clientId = s;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String s) {
            this.clientSecret = s;
        }

        public String getRedirectUri() {
            return redirectUri;
        }

        public void setRedirectUri(String s) {
            this.redirectUri = s;
        }
    }

    public static class Security {
        private String jwtSecret;
        private int jwtExpiryMinutes = 60;
        private String tokenEncryptionKey;

        public String getJwtSecret() {
            return jwtSecret;
        }

        public void setJwtSecret(String s) {
            this.jwtSecret = s;
        }

        public int getJwtExpiryMinutes() {
            return jwtExpiryMinutes;
        }

        public void setJwtExpiryMinutes(int m) {
            this.jwtExpiryMinutes = m;
        }

        public String getTokenEncryptionKey() {
            return tokenEncryptionKey;
        }

        public void setTokenEncryptionKey(String s) {
            this.tokenEncryptionKey = s;
        }
    }

    public static class Coordination {
        private int approvalTimeoutMinutes = 720;
        private int maxAvailabilityWindowDays = 14;

        public int getApprovalTimeoutMinutes() {
            return approvalTimeoutMinutes;
        }

        public void setApprovalTimeoutMinutes(int m) {
            this.approvalTimeoutMinutes = m;
        }

        public int getMaxAvailabilityWindowDays() {
            return maxAvailabilityWindowDays;
        }

        public void setMaxAvailabilityWindowDays(int d) {
            this.maxAvailabilityWindowDays = d;
        }
    }

    public static class Llm {
        private boolean enabled = true;
        private String groqApiKey;
        private String model = "llama3-8b-8192";
        private int timeoutMs = 5000;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean e) {
            this.enabled = e;
        }

        public String getGroqApiKey() {
            return groqApiKey;
        }

        public void setGroqApiKey(String k) {
            this.groqApiKey = k;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String m) {
            this.model = m;
        }

        public int getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(int t) {
            this.timeoutMs = t;
        }
    }

    public static class RateLimiting {
        private int requestsPerMinute = 100;

        public int getRequestsPerMinute() {
            return requestsPerMinute;
        }

        public void setRequestsPerMinute(int r) {
            this.requestsPerMinute = r;
        }
    }
}
