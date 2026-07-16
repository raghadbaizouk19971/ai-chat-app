package com.raghad.aichat.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ChatRequest(
        @NotBlank @Size(max = 4000) String prompt,
        @Size(max = 40) List<@NotNull @Valid Message> history
) {
    public ChatRequest {
        if (history == null) {
            history = List.of();
        }
    }
}
