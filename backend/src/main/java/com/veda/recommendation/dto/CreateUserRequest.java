package com.veda.recommendation.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateUserRequest(
        @NotBlank String username,
        String ageGroup,
        String country
) {
}
