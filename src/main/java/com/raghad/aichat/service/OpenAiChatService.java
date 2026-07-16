package com.raghad.aichat.service;

import com.raghad.aichat.config.OpenAiProperties;
import com.raghad.aichat.dto.Message;
import com.raghad.aichat.exception.UpstreamApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.Exceptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Service
public class OpenAiChatService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiChatService.class);

    private final WebClient webClient;
    private final OpenAiProperties properties;

    public OpenAiChatService(WebClient openAiWebClient, OpenAiProperties properties) {
        this.webClient = openAiWebClient;
        this.properties = properties;
    }

    public String getChatReply(String prompt, List<Message> history) {
        List<Map<String, String>> messages = new ArrayList<>();
        for (Message m : history) {
            messages.add(Map.of("role", m.role(), "content", m.content()));
        }
        messages.add(Map.of("role", "user", "content", prompt));

        Map<String, Object> body = Map.of(
                "model", properties.getModel(),
                "messages", messages
        );

        try {
            Map<String, Object> response = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(properties.getTimeout())
                    .block();

            return extractReply(response);

        } catch (WebClientResponseException ex) {
            log.warn("Upstream API returned error status {}", ex.getStatusCode());
            String reason = switch (ex.getStatusCode().value()) {
                case 401 -> "Upstream API rejected the request: invalid API key.";
                case 403 -> "Upstream API rejected access to the configured model.";
                case 429 -> "Upstream API rate limit exceeded. Please try again shortly.";
                default -> "Upstream API returned an error (status " + ex.getStatusCode().value() + ").";
            };
            throw new UpstreamApiException(reason, ex.getStatusCode());

        } catch (WebClientRequestException ex) {
            log.warn("Failed to reach upstream API: {}", ex.getMessage());
            throw new UpstreamApiException("Could not reach the upstream service. Please try again.", ex);

        } catch (UpstreamApiException ex) {
            throw ex;

        } catch (RuntimeException ex) {
            Throwable cause = Exceptions.unwrap(ex);
            if (cause instanceof TimeoutException) {
                log.warn("Upstream API timed out after {}", properties.getTimeout());
                throw new UpstreamApiException(
                        "The upstream service took too long to respond. Please try again.",
                        ex,
                        true
                );
            }
            log.warn("Unexpected error calling upstream API: {}", ex.getMessage());
            throw new UpstreamApiException("The upstream service failed unexpectedly. Please try again.", ex);
        }
    }

    @SuppressWarnings("unchecked")
    private String extractReply(Map<String, Object> response) {
        if (response == null) {
            throw new UpstreamApiException("Empty response from upstream API.", (org.springframework.http.HttpStatusCode) null);
        }
        try {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            Map<String, Object> firstChoice = choices.get(0);
            Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
            String content = (String) message.get("content");
            if (content == null || content.isBlank()) {
                throw new IllegalArgumentException("Missing assistant content");
            }
            return content;
        } catch (Exception e) {
            log.error("Unexpected response shape from upstream API");
            throw new UpstreamApiException("Unexpected response format from upstream API.", (org.springframework.http.HttpStatusCode) null);
        }
    }
}
