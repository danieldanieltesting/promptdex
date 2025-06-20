package com.promptdex.api.controller;

import com.promptdex.api.dto.PromptDto;
import com.promptdex.api.service.PromptService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final PromptService promptService;

    public UserController(PromptService promptService) {
        this.promptService = promptService;
    }

    @GetMapping("/{username}/prompts")
    public ResponseEntity<Page<PromptDto>> getPromptsByUser(
            @PathVariable String username,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "9") int size
    ) {
        Page<PromptDto> promptsPage = promptService.getPromptsByAuthorUsername(username, page, size);
        return ResponseEntity.ok(promptsPage);
    }
}