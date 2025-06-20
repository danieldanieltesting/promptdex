package com.promptdex.api.repository;

import com.promptdex.api.model.Prompt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PromptRepository extends JpaRepository<Prompt, UUID> {

    /**
     * Searches for prompts with pagination and filtering by search term and tags.
     * This query is optimized to prevent the N+1 problem by fetching the author
     * and tags relationships in the same query as the prompts.
     */
    @Query(value = "SELECT DISTINCT p FROM Prompt p " +
            "LEFT JOIN FETCH p.author " +
            "LEFT JOIN FETCH p.tags t WHERE " +
            "(:searchTerm IS NULL OR :searchTerm = '' OR " +
            "LOWER(p.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(p.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(p.promptText) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND " +
            "(:tags IS NULL OR t.name IN :tags)",
            countQuery = "SELECT COUNT(DISTINCT p) FROM Prompt p LEFT JOIN p.tags t WHERE " +
                    "(:searchTerm IS NULL OR :searchTerm = '' OR " +
                    "LOWER(p.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
                    "LOWER(p.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
                    "LOWER(p.promptText) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND " +
                    "(:tags IS NULL OR t.name IN :tags)")
    Page<Prompt> searchAndPagePrompts(
            @Param("searchTerm") String searchTerm,
            @Param("tags") List<String> tags,
            Pageable pageable
    );

    Page<Prompt> findByAuthor_Username(String username, Pageable pageable);

    Page<Prompt> findByBookmarkedByUsers_Username(String username, Pageable pageable);
}