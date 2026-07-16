package com.raghad.aichat.service;

import com.raghad.aichat.config.OpenAiProperties;
import com.raghad.aichat.exception.UpstreamApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenAiChatServiceTest {

    @Test
    void extractsReplyFromCompatibleResponse() {
        ClientResponse response = ClientResponse.create(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body("""
                        {"choices":[{"message":{"role":"assistant","content":"Hello!"}}]}
                        """)
                .build();

        OpenAiChatService service = serviceWith(request -> Mono.just(response), Duration.ofSeconds(1));

        assertThat(service.getChatReply("Hi", List.of())).isEqualTo("Hello!");
    }

    @Test
    void preservesUpstreamRateLimitStatus() {
        ClientResponse response = ClientResponse.create(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body("""
                        {"error":{"message":"rate limited"}}
                        """)
                .build();

        OpenAiChatService service = serviceWith(request -> Mono.just(response), Duration.ofSeconds(1));

        assertThatThrownBy(() -> service.getChatReply("Hi", List.of()))
                .isInstanceOfSatisfying(UpstreamApiException.class, error -> {
                    assertThat(error.getUpstreamStatus().value()).isEqualTo(429);
                    assertThat(error.getMessage()).contains("rate limit");
                });
    }

    @Test
    void identifiesTimeoutsSeparatelyFromConnectionFailures() {
        OpenAiChatService service = serviceWith(request -> Mono.never(), Duration.ofMillis(20));

        assertThatThrownBy(() -> service.getChatReply("Hi", List.of()))
                .isInstanceOfSatisfying(UpstreamApiException.class, error -> {
                    assertThat(error.isTimeout()).isTrue();
                    assertThat(error.getMessage()).contains("too long");
                });
    }

    private OpenAiChatService serviceWith(ExchangeFunction exchangeFunction, Duration timeout) {
        OpenAiProperties properties = new OpenAiProperties();
        properties.setBaseUrl("http://localhost/v1");
        properties.setModel("test-model");
        properties.setTimeout(timeout);

        WebClient webClient = WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .exchangeFunction(exchangeFunction)
                .build();

        return new OpenAiChatService(webClient, properties);
    }
}
