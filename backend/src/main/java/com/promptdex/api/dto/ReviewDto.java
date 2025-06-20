// src/main/java/com/promptdex/api/dto/ReviewDto.java
package com.promptdex.api.dto;

import java.time.Instant;
import java.util.UUID;

// This record defines the data the backend SENDS BACK to the frontend.
// It includes server-generated info like the id, username, and timestamps.
public record ReviewDto(
        UUID id,
        int rating,
        String comment,
        String authorUsername, // Changed from reviewerUsername for consistency with PromptDto
        Instant createdAt,
        Instant updatedAt // --- NEW FIELD ---
) {}