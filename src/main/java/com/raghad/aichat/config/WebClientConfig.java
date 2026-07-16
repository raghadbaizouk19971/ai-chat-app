package com.raghad.aichat.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(OpenAiProperties.class)
public class WebClientConfig {

    @Bean
    public WebClient openAiWebClient(OpenAiProperties properties) {
        WebClient.Builder builder = WebClient.builder()
                .baseUrl(properties.getBaseUrl());

        if (properties.getApiKey() != null && !properties.getApiKey().isBlank()) {
            builder = builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey());
        }

        return builder.build();
    }
}
