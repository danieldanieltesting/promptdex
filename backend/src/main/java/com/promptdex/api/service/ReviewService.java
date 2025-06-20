package com.promptdex.api.service;

import com.promptdex.api.dto.CreateReviewRequest;
import com.promptdex.api.dto.ReviewDto;
import com.promptdex.api.exception.ResourceNotFoundException;
import com.promptdex.api.exception.ReviewAlreadyExistsException;
import com.promptdex.api.model.Prompt;
import com.promptdex.api.model.Review;
import com.promptdex.api.model.User;
import com.promptdex.api.repository.PromptRepository;
import com.promptdex.api.repository.ReviewRepository;
import com.promptdex.api.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final PromptRepository promptRepository;
    private final UserRepository userRepository;

    public ReviewService(ReviewRepository reviewRepository, PromptRepository promptRepository, UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.promptRepository = promptRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ReviewDto createReview(CreateReviewRequest createReviewRequest, String username) {
        // 1. Find the parent entities
        Prompt prompt = promptRepository.findById(createReviewRequest.promptId())
                .orElseThrow(() -> new ResourceNotFoundException("Prompt not found with id: " + createReviewRequest.promptId()));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));

        // 2. Enforce "One Review Per User" constraint BEFORE creating the new review
        if (reviewRepository.existsByPrompt_IdAndUser_Id(prompt.getId(), user.getId())) {
            throw new ReviewAlreadyExistsException("User has already submitted a review for this prompt.");
        }

        // 3. Create the new Review entity
        Review review = new Review();
        review.setRating(createReviewRequest.rating());
        review.setComment(createReviewRequest.comment());
        review.setUser(user);
        review.setPrompt(prompt);

        // 4. Save the entity.
        Review savedReview = reviewRepository.save(review);

        // 5. Convert the saved entity to our output DTO
        return convertToDto(savedReview);
    }

    private ReviewDto convertToDto(Review review) {
        Instant createdAtInstant = (review.getCreatedAt() != null)
                ? review.getCreatedAt().toInstant(ZoneOffset.UTC)
                : null; // It's safer to return null if the source is null

        return new ReviewDto(
                review.getId(),
                review.getRating(),
                review.getComment(),
                review.getUser().getUsername(),
                createdAtInstant
        );
    }
}