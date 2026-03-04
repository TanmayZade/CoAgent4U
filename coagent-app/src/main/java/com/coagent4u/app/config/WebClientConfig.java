package com.coagent4u.app.config;

import java.time.Duration;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.channel.ChannelOption;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

/**
 * Shared WebClient configuration for all outbound HTTP calls.
 * Provides:
 * <ul>
 * <li>Connect timeout: 3 seconds</li>
 * <li>Read timeout: 5 seconds</li>
 * <li>Retry: max 2 attempts with 500ms exponential backoff</li>
 * <li>Correlation ID propagation via X-Correlation-Id header</li>
 * </ul>
 */
@Configuration
public class WebClientConfig {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    @Bean
    public WebClient.Builder webClientBuilder() {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(5))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000);

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .filter(correlationIdFilter())
                .filter(retryFilter());
    }

    /**
     * Propagates the MDC correlation ID (or generates one) on every outbound
     * request.
     */
    private ExchangeFilterFunction correlationIdFilter() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            String correlationId = MDC.get("correlationId");
            if (correlationId == null || correlationId.isBlank()) {
                correlationId = UUID.randomUUID().toString().substring(0, 8);
            }
            return Mono.just(
                    ClientRequest.from(request)
                            .header(CORRELATION_ID_HEADER, correlationId)
                            .build());
        });
    }

    /**
     * Retries failed requests up to 2 times with exponential backoff (500ms base).
     * Only retries on 5xx server errors.
     */
    private ExchangeFilterFunction retryFilter() {
        return (request, next) -> next.exchange(request)
                .flatMap(response -> {
                    if (response.statusCode().is5xxServerError()) {
                        return response.releaseBody()
                                .then(Mono.error(new RuntimeException(
                                        "Server error: " + response.statusCode())));
                    }
                    return Mono.just(response);
                })
                .retryWhen(Retry.backoff(2, Duration.ofMillis(500))
                        .onRetryExhaustedThrow((spec, signal) -> signal.failure()));
    }
}
