package com.veda.recommendation.dto;

public record DemoResetResponse(
        Long demoUserId,
        int usersCreated,
        int contentCreated,
        int eventsCreated,
        String message
) {
}
