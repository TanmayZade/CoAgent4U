package com.coagent4u.app.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.coagent4u.app.filter.JwtAuthenticationFilter;
import com.coagent4u.app.filter.RateLimitFilter;
import com.coagent4u.app.security.GoogleOAuthStateService;
import com.coagent4u.config.CoagentProperties;
import com.coagent4u.security.CaffeineRateLimiter;
import com.coagent4u.security.JwtTokenBlacklist;
import com.coagent4u.security.JwtValidator;

/**
 * Filter registration and supporting bean configuration.
 * Ensures JWT authentication filter runs before rate limiting filter.
 */
@Configuration
public class FilterRegistrationConfig {

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
            JwtValidator jwtValidator,
            JwtTokenBlacklist tokenBlacklist) {
        return new JwtAuthenticationFilter(jwtValidator, tokenBlacklist);
    }

    @Bean
    public RateLimitFilter rateLimitFilter(CaffeineRateLimiter rateLimiter) {
        return new RateLimitFilter(rateLimiter);
    }

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(
            JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setOrder(1); // Run first
        reg.addUrlPatterns("/*");
        return reg;
    }

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(
            RateLimitFilter filter) {
        FilterRegistrationBean<RateLimitFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setOrder(2); // Run after JWT filter
        reg.addUrlPatterns("/*");
        return reg;
    }

    @Bean
    public GoogleOAuthStateService googleOAuthStateService(CoagentProperties props) {
        return new GoogleOAuthStateService(props.getSecurity().getJwtSecret());
    }
}
