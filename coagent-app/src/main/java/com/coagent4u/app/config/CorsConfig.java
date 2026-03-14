package com.coagent4u.app.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.coagent4u.config.CoagentProperties;

/**
 * CORS configuration for frontend development.
 * Allows credentials (for HTTPOnly cookies) from the frontend origin.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final CoagentProperties properties;

    public CorsConfig(CoagentProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String frontendUrl = properties.getFrontendUrl();
        // Extract base domain for pattern matching if using a custom domain
        String domainPattern = frontendUrl.replace("https://", "https://*.")
                                         .replace("http://", "http://*.");

        registry.addMapping("/**")
                .allowedOrigins(frontendUrl, "https://www.coagent4u.com", "https://coagent4u.com")
                .allowedOriginPatterns(domainPattern, "https://*.coagent4u.com")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
