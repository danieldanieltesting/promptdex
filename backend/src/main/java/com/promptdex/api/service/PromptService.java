// src/main/java/com/promptdex/api/service/PromptService.java
package com.promptdex.api.service;

import com.promptdex.api.dto.CreatePromptRequest;
import com.promptdex.api.dto.PromptDto;
import com.promptdex.api.exception.ResourceNotFoundException;
import com.promptdex.api.mapper.PromptMapper; // <-- IMPORT THE MAPPER
import com.promptdex.api.model.Prompt;
import com.promptdex.api.model.Tag;
import com.promptdex.api.model.User;
import com.promptdex.api.repository.PromptRepository;
import com.promptdex.api.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional // Apply transactionality at the class level
public class PromptService {

    private final PromptRepository promptRepository;
    private final UserRepository userRepository;
    private final TagService tagService;
    private final PromptMapper promptMapper; // <-- NEW DEPENDENCY

    // --- CONSTRUCTOR UPDATED ---
    public PromptService(PromptRepository promptRepository, UserRepository userRepository, TagService tagService, PromptMapper promptMapper) {
        this.promptRepository = promptRepository;
        this.userRepository = userRepository;
        this.tagService = tagService;
        this.promptMapper = promptMapper; // <-- INJECT THE MAPPER
    }

    private User getOptionalUser(UserDetails userDetails) {
        if (userDetails == null) {
            return null;
        }
        // Use the non-bookmark-fetching method for efficiency unless bookmarks are needed
        return userRepository.findByUsername(userDetails.getUsername()).orElse(null);
    }

    @Transactional(readOnly = true)
    public Page<PromptDto> searchAndPagePrompts(String searchTerm, List<String> tags, int page, int size, UserDetails userDetails) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<String> lowerCaseTags = (tags != null && !tags.isEmpty()) ? tags.stream().map(String::toLowerCase).collect(Collectors.toList()) : null;
        Page<Prompt> promptPage = promptRepository.searchAndPagePrompts(searchTerm, lowerCaseTags, pageable);

        // --- REFACTORED TO USE MAPPER ---
        User currentUser = getOptionalUser(userDetails);
        return promptPage.map(prompt -> promptMapper.toDto(prompt, currentUser));
    }

    @Transactional(readOnly = true)
    public PromptDto getPromptById(UUID promptId, UserDetails userDetails) {
        Prompt prompt = promptRepository.findById(promptId)
                .orElseThrow(() -> new ResourceNotFoundException("Prompt not found with id: " + promptId));

        // --- REFACTORED TO USE MAPPER ---
        User currentUser = getOptionalUser(userDetails);
        return promptMapper.toDto(prompt, currentUser);
    }

    @Transactional
    public PromptDto createPrompt(CreatePromptRequest request, UserDetails userDetails) {
        User user = getOptionalUser(userDetails);
        if (user == null) {
            throw new ResourceNotFoundException("User not found to create prompt.");
        }

        Prompt prompt = new Prompt();
        prompt.setTitle(request.title());
        prompt.setPromptText(request.text());
        prompt.setDescription(request.description());
        prompt.setTargetAiModel(request.model());
        prompt.setCategory(request.category());
        prompt.setAuthor(user);

        Prompt savedPrompt = promptRepository.save(prompt);

        // --- REFACTORED TO USE MAPPER ---
        return promptMapper.toDto(savedPrompt, user);
    }

    @Transactional
    public PromptDto updatePrompt(UUID promptId, CreatePromptRequest request, UserDetails userDetails) throws AccessDeniedException {
        User user = getOptionalUser(userDetails);
        Prompt prompt = promptRepository.findById(promptId)
                .orElseThrow(() -> new ResourceNotFoundException("Prompt not found with id: " + promptId));

        if (user == null || !prompt.getAuthor().getId().equals(user.getId())) {
            throw new AccessDeniedException("You do not have permission to edit this prompt.");
        }

        prompt.setTitle(request.title());
        prompt.setPromptText(request.text());
        prompt.setDescription(request.description());
        prompt.setTargetAiModel(request.model());
        prompt.setCategory(request.category());

        Prompt updatedPrompt = promptRepository.save(prompt);

        // --- REFACTORED TO USE MAPPER ---
        return promptMapper.toDto(updatedPrompt, user);
    }

    @Transactional
    public PromptDto updatePromptTags(UUID promptId, Set<String> tagNames, UserDetails userDetails) throws AccessDeniedException {
        User user = getOptionalUser(userDetails);
        Prompt prompt = promptRepository.findById(promptId)
                .orElseThrow(() -> new ResourceNotFoundException("Prompt not found with id: " + promptId));

        if (user == null || !prompt.getAuthor().getId().equals(user.getId())) {
            throw new AccessDeniedException("You do not have permission to edit tags for this prompt.");
        }

        Set<Tag> managedTags = tagService.findOrCreateTags(tagNames);
        prompt.setTags(managedTags);
        Prompt savedPrompt = promptRepository.save(prompt);

        // --- REFACTORED TO USE MAPPER ---
        return promptMapper.toDto(savedPrompt, user);
    }

    @Transactional
    public void deletePrompt(UUID promptId, UserDetails userDetails) throws AccessDeniedException {
        User user = getOptionalUser(userDetails);
        Prompt prompt = promptRepository.findById(promptId)
                .orElseThrow(() -> new ResourceNotFoundException("Prompt not found with id: " + promptId));

        if (user == null || !prompt.getAuthor().getId().equals(user.getId())) {
            throw new AccessDeniedException("You do not have permission to delete this prompt.");
        }
        promptRepository.delete(prompt);
    }

    // --- BOOKMARKING AND OTHER METHODS ---

    @Transactional
    public void addBookmark(UUID promptId, String username) {
        User user = userRepository.findByUsernameWithBookmarks(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        Prompt prompt = promptRepository.findById(promptId)
                .orElseThrow(() -> new ResourceNotFoundException("Prompt not found with id: " + promptId));
        user.getBookmarkedPrompts().add(prompt);
    }

    @Transactional
    public void removeBookmark(UUID promptId, String username) {
        User user = userRepository.findByUsernameWithBookmarks(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        Prompt prompt = promptRepository.findById(promptId)
                .orElseThrow(() -> new ResourceNotFoundException("Prompt not found with id: " + promptId));
        user.getBookmarkedPrompts().remove(prompt);
    }

    @Transactional(readOnly = true)
    public Page<PromptDto> getBookmarkedPrompts(String username, int page, int size) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Prompt> promptPage = promptRepository.findByBookmarkedByUsers_Username(username, pageable);

        // --- REFACTORED TO USE MAPPER ---
        return promptPage.map(prompt -> promptMapper.toDto(prompt, user));
    }

    @Transactional(readOnly = true)
    public Page<PromptDto> getPromptsByAuthorUsername(String username, int page, int size, UserDetails userDetails) {
        userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Prompt> promptPage = promptRepository.findByAuthor_Username(username, pageable);

        User currentUser = getOptionalUser(userDetails);
        return promptPage.map(prompt -> promptMapper.toDto(prompt, currentUser));
    }

    // --- THE REDUNDANT METHOD HAS BEEN COMPLETELY REMOVED ---
    // private PromptDto convertToDto(Prompt prompt, Set<UUID> bookmarkedPromptIds) { ... }
}