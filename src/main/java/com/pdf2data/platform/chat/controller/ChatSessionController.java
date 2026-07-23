package com.pdf2data.platform.chat.controller;

import com.pdf2data.platform.auth.entity.User;
import com.pdf2data.platform.auth.repository.UserRepository;
import com.pdf2data.platform.chat.dto.ChatSessionDetailsResponse;
import com.pdf2data.platform.chat.dto.ChatSessionResponse;
import com.pdf2data.platform.chat.service.ChatSessionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatSessionController {

    private final ChatSessionService chatSessionService;
    private final UserRepository userRepository;

    public ChatSessionController(
            ChatSessionService chatSessionService,
            UserRepository userRepository
    ) {
        this.chatSessionService = chatSessionService;
        this.userRepository = userRepository;
    }

    /*
     * Sidebar Chat History
     */
    @GetMapping("/sessions")
    public ResponseEntity<?> getSessions() {

        try {

            User user = currentUser();

            List<ChatSessionResponse> chats =
                    chatSessionService.getUserChats(user.getId());

            return ResponseEntity.ok(chats);

        } catch (Exception e) {

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());

        }
    }

    /*
     * Open Existing Chat
     */
    @GetMapping("/session/{id}")
    public ResponseEntity<?> getSession(
            @PathVariable Long id
    ) {

        try {

            User user = currentUser();

            ChatSessionDetailsResponse response =
                    chatSessionService.getSessionDetails(
                            id,
                            user.getId()
                    );

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());

        }
    }

    /*
     * Rename Chat
     */
    @PatchMapping("/session/{id}/rename")
    public ResponseEntity<?> rename(
            @PathVariable Long id,
            @RequestParam String title
    ) {

        try {

            chatSessionService.renameChat(
                    id,
                    title
            );

            return ResponseEntity.ok(
                    "Chat renamed successfully."
            );

        } catch (Exception e) {

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());

        }
    }

    /*
     * Pin / Unpin Chat
     */
    @PatchMapping("/session/{id}/pin")
    public ResponseEntity<?> pin(
            @PathVariable Long id
    ) {

        try {

            chatSessionService.togglePin(id);

            return ResponseEntity.ok(
                    "Chat pin updated."
            );

        } catch (Exception e) {

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());

        }
    }

    /*
     * Delete Chat
     */
    @DeleteMapping("/session/{id}")
    public ResponseEntity<?> delete(
            @PathVariable Long id
    ) {

        try {

            chatSessionService.deleteChat(id);

            return ResponseEntity.ok(
                    "Chat deleted successfully."
            );

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