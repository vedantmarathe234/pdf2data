package com.pdf2data.platform.chat.controller;

import com.pdf2data.platform.auth.entity.User;
import com.pdf2data.platform.auth.repository.UserRepository;
import com.pdf2data.platform.chat.dto.ChatRequest;
import com.pdf2data.platform.chat.dto.ChatResponse;
import com.pdf2data.platform.chat.entity.ChatHistory;
import com.pdf2data.platform.chat.service.ChatService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatService chatService;
    private final UserRepository userRepository;

    public ChatController(
            ChatService chatService,
            UserRepository userRepository
    ) {
        this.chatService = chatService;
        this.userRepository = userRepository;
    }

    /*
     * Ask AI
     */
    @PostMapping("/ask")
    public ResponseEntity<?> ask(
            @RequestBody ChatRequest request
    ) {

        try {

            User user = currentUser();

            ChatResponse response =
                    chatService.ask(
                            request.getChatSessionId(),
                            user.getId(),
                            request.getMessage()
                    );

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());

        }

    }

    /*
     * Chat Messages History
     */
    @GetMapping("/history/{chatSessionId}")
    public ResponseEntity<?> history(
            @PathVariable Long chatSessionId
    ) {

        try {

            User user = currentUser();

            List<ChatHistory> history =
                    chatService.getHistory(
                            chatSessionId,
                            user.getId()
                    );

            return ResponseEntity.ok(history);

        } catch (Exception e) {

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());

        }

    }

    /*
     * Logged-in User
     */
    private User currentUser() {

        String email =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getName();

        return userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

    }

}