package com.coagent4u.messaging;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;
import java.time.Duration;

import com.coagent4u.agent.port.out.PythonAgentPort;
import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.UserId;

@Component
public class PythonAgentAdapter implements PythonAgentPort {

    private static final Logger log = LoggerFactory.getLogger(PythonAgentAdapter.class);

    private final WebClient webClient;

    public PythonAgentAdapter(
            WebClient.Builder webClientBuilder,
            @Value("${python.agent.url:http://localhost:8000}") String pythonAgentUrl) {
        this.webClient = webClientBuilder
                .baseUrl(pythonAgentUrl)
                .build();
    }

    @Override
    public String forwardToPython(AgentId agentId, UserId userId, String rawText) {
        log.info("[PythonAdapter] Forwarding message to Python agent={}", agentId.value());

        Map<String, String> request = Map.of(
                "agent_id", agentId.value().toString(),
                "user_id", userId.value().toString(),
                "raw_text", rawText
        );

        try {
            PythonAgentResponse response = webClient.post()
                    .uri("/agent/handle")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(PythonAgentResponse.class)
                    .timeout(Duration.ofSeconds(45)) // LLMs can take a while
                    .retryWhen(Retry.fixedDelay(1, Duration.ofSeconds(2)))
                    .block();

            if (response != null && response.message() != null) {
                log.info("[PythonAdapter] Received response via={}", response.via());
                return response.message();
            }
            
            log.warn("[PythonAdapter] Received empty response from Python");
            return "I'm sorry, my brain returned an empty response. Please try again.";

        } catch (Exception e) {
            log.error("[PythonAdapter] Failed to communicate with Python agent: {}", e.getMessage(), e);
            return "I'm having trouble connecting to my reasoning engine right now. Please try again in a moment.";
        }
    }

    // DTO matching HandleMessageResponse from Python
    private record PythonAgentResponse(String message, String via, java.util.List<String> tools_called) {}
}
