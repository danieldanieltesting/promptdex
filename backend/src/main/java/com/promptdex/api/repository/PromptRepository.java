package com.promptdex.api.repository;

import com.promptdex.api.model.Prompt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface PromptRepository extends JpaRepository<Prompt, UUID> {

    /**
     * Finds a paginated list of prompts, optionally filtered by a search term.
     * The search is case-insensitive and checks the prompt's title, description, and text.
     * If the search term is null or empty, it returns all prompts (paginated).
     *
     * @param searchTerm The term to search for. Can be null or empty.
     * @param pageable   The pagination information (page number, size, sort order).
     * @return A Page of Prompts matching the criteria.
     */
    @Query("SELECT p FROM Prompt p WHERE " +
            "(:searchTerm IS NULL OR :searchTerm = '' OR " +
            "LOWER(p.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(p.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(p.promptText) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Prompt> searchAndPagePrompts(@Param("searchTerm") String searchTerm, Pageable pageable);

}