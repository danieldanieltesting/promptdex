package com.promptdex.api.repository;

import com.promptdex.api.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    /**
     * Checks if a review exists for a given prompt ID and user ID.
     * This is used to enforce the "one review per user per prompt" rule.
     *
     * @param promptId The ID of the prompt.
     * @param userId   The ID of the user.
     * @return true if a review exists, false otherwise.
     */
    boolean existsByPrompt_IdAndUser_Id(UUID promptId, UUID userId);
}