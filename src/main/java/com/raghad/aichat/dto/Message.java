package com.raghad.aichat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record Message(
        @NotBlank
        @Pattern(regexp = "system|user|assistant", message = "must be system, user, or assistant")
        String role,
        @NotBlank @Size(max = 8000) String content
) {
}
