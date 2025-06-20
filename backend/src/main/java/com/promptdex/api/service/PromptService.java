package com.promptdex.api.service;

import com.promptdex.api.dto.CreatePromptRequest;
import com.promptdex.api.dto.PromptDto;
import com.promptdex.api.dto.ReviewDto;
import com.promptdex.api.exception.ResourceNotFoundException;
import com.promptdex.api.model.Prompt;
import com.promptdex.api.model.Review;
import com.promptdex.api.model.User;
import com.promptdex.api.repository.PromptRepository;
import com.promptdex.api.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.AccessDeniedException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PromptService {

    private final PromptRepository promptRepository;
    private final UserRepository userRepository;

    public PromptService(PromptRepository promptRepository, UserRepository userRepository) {
        this.promptRepository = promptRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<PromptDto> searchAndPagePrompts(String searchTerm, int page, int size) {
        // Create a Pageable object to define the page number, size, and sorting order.
        // We will sort by creation date in descending order by default.
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        // Call the new repository method
        Page<Prompt> promptPage = promptRepository.searchAndPagePrompts(searchTerm, pageable);

        // Map the Page<Prompt> to a Page<PromptDto> using the existing converter
        return promptPage.map(this::convertToDto);
    }

    @Transactional(readOnly = true)
    public PromptDto getPromptById(UUID promptId) {
        Prompt prompt = promptRepository.findById(promptId)
                .orElseThrow(() -> new ResourceNotFoundException("Prompt not found with id: " + promptId));
        return convertToDto(prompt);
    }

    @Transactional
    public PromptDto createPrompt(CreatePromptRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));

        Prompt prompt = new Prompt();
        prompt.setTitle(request.title());
        prompt.setPromptText(request.text());
        prompt.setDescription(request.description());
        prompt.setTargetAiModel(request.model());
        prompt.setCategory(request.category());
        prompt.setAuthor(user);

        Prompt savedPrompt = promptRepository.saveAndFlush(prompt);

        return convertToDto(savedPrompt);
    }

    @Transactional
    public PromptDto updatePrompt(UUID promptId, CreatePromptRequest request, String username) throws AccessDeniedException {
        Prompt prompt = promptRepository.findById(promptId)
                .orElseThrow(() -> new ResourceNotFoundException("Prompt not found with id: " + promptId));

        if (!prompt.getAuthor().getUsername().equals(username)) {
            throw new AccessDeniedException("You do not have permission to edit this prompt.");
        }

        prompt.setTitle(request.title());
        prompt.setPromptText(request.text());
        prompt.setDescription(request.description());
        prompt.setTargetAiModel(request.model());
        prompt.setCategory(request.category());

        Prompt updatedPrompt = promptRepository.save(prompt);
        return convertToDto(updatedPrompt);
    }

    @Transactional
    public void deletePrompt(UUID promptId, String username) throws AccessDeniedException {
        Prompt prompt = promptRepository.findById(promptId)
                .orElseThrow(() -> new ResourceNotFoundException("Prompt not found with id: " + promptId));

        if (!prompt.getAuthor().getUsername().equals(username)) {
            throw new AccessDeniedException("You do not have permission to delete this prompt.");
        }

        promptRepository.delete(prompt);
    }

    private PromptDto convertToDto(Prompt prompt) {
        LocalDateTime createdAt = prompt.getCreatedAt();
        LocalDateTime updatedAt = prompt.getUpdatedAt();

        Instant createdAtInstant = (createdAt != null) ? createdAt.toInstant(ZoneOffset.UTC) : null;
        Instant updatedAtInstant = (updatedAt != null) ? updatedAt.toInstant(ZoneOffset.UTC) : null;

        List<Review> reviews = prompt.getReviews();

        Double averageRating = (reviews != null && !reviews.isEmpty())
                ? reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0)
                : 0.0;

        List<ReviewDto> reviewDtos = (reviews != null)
                ? reviews.stream()
                .map(review -> {
                    LocalDateTime reviewCreatedAt = review.getCreatedAt();
                    Instant reviewCreatedAtInstant = (reviewCreatedAt != null) ? reviewCreatedAt.toInstant(ZoneOffset.UTC) : null;

                    return new ReviewDto(
                            review.getId(),
                            review.getRating(),
                            review.getComment(),
                            review.getUser().getUsername(),
                            reviewCreatedAtInstant
                    );
                })
                .collect(Collectors.toList())
                : Collections.emptyList();

        return new PromptDto(
                prompt.getId(),
                prompt.getTitle(),
                prompt.getPromptText(),
                prompt.getDescription(),
                prompt.getTargetAiModel(),
                prompt.getCategory(),
                prompt.getAuthor().getUsername(),
                createdAtInstant,
                updatedAtInstant,
                averageRating,
                reviewDtos
        );
    }
}