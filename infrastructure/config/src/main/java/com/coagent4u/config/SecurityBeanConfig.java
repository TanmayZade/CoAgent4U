package com.coagent4u.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.coagent4u.security.*;

/**
 * Wires security beans from {@link CoagentProperties} with fail-fast
 * validation.
 * If any required secret is missing, the application fails to boot.
 */
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
