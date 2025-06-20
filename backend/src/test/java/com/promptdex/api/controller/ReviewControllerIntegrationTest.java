package com.promptdex.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.promptdex.api.model.Prompt;
import com.promptdex.api.model.User;
import com.promptdex.api.repository.PromptRepository;
import com.promptdex.api.repository.ReviewRepository;
import com.promptdex.api.repository.UserRepository;
import com.promptdex.api.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
public class ReviewControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PromptRepository promptRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String jwtToken;
    private User testUser;
    private Prompt testPrompt;

    @BeforeEach
    void setUp() {
        reviewRepository.deleteAll();
        promptRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword(passwordEncoder.encode("password"));
        userRepository.save(testUser);

        testPrompt = new Prompt();
        testPrompt.setTitle("Test Prompt");
        testPrompt.setPromptText("A prompt for testing.");
        testPrompt.setTargetAiModel("GPT-4");
        testPrompt.setCategory("Testing");
        testPrompt.setAuthor(testUser);
        promptRepository.save(testPrompt);

        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                testUser.getUsername(),
                testUser.getPassword(),
                new ArrayList<>()
        );
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
        jwtToken = jwtTokenProvider.generateToken(authentication);
    }

    @Test
    void createReview_whenNotAlreadyReviewed_shouldSucceedAndReturn201() throws Exception {
        // Arrange
        Map<String, Object> reviewRequest = new HashMap<>();
        reviewRequest.put("promptId", testPrompt.getId());
        reviewRequest.put("rating", 4);
        reviewRequest.put("comment", "This is a great prompt!");

        // Act & Assert
        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(4))
                .andExpect(jsonPath("$.comment").value("This is a great prompt!"))
                // FIX: Assert on 'authorUsername' instead of 'username'
                .andExpect(jsonPath("$.authorUsername").value("testuser"));
    }

    @Test
    void createReview_whenAlreadyReviewed_shouldFailAndReturn409() throws Exception {
        // Arrange: Create one successful review
        Map<String, Object> firstReviewRequest = new HashMap<>();
        firstReviewRequest.put("promptId", testPrompt.getId());
        firstReviewRequest.put("rating", 5);
        firstReviewRequest.put("comment", "My first review.");

        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstReviewRequest)))
                .andExpect(status().isCreated());

        // Prepare the second review attempt
        Map<String, Object> secondReviewRequest = new HashMap<>();
        secondReviewRequest.put("promptId", testPrompt.getId());
        secondReviewRequest.put("rating", 1);
        secondReviewRequest.put("comment", "Trying to review again.");

        // Act & Assert: Expect a 409 Conflict
        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondReviewRequest)))
                .andExpect(status().isConflict())
                // FIX: Assert on 'statusCode' instead of 'status'
                .andExpect(jsonPath("$.statusCode").value(409))
                .andExpect(jsonPath("$.message").value("User has already submitted a review for this prompt."));
    }
}