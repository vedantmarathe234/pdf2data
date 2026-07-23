package com.pdf2data.platform.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pdf2data.platform.chat.dto.ChatResponse;
import com.pdf2data.platform.chat.entity.ChatHistory;
import com.pdf2data.platform.chat.repository.ChatHistoryRepository;
import com.pdf2data.platform.document.entity.MongoExtractionResult;
import com.pdf2data.platform.document.repository.MongoExtractionRepository;
import com.pdf2data.platform.document.service.AiExtractionService;
import org.springframework.stereotype.Service;
import com.pdf2data.platform.chat.entity.ChatDocument;
import com.pdf2data.platform.chat.repository.ChatDocumentRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private static final int MAX_HISTORY_MESSAGES = 10;
    private final ChatHistoryRepository chatHistoryRepository;
    private final ChatDocumentRepository chatDocumentRepository;
    private final MongoExtractionRepository mongoExtractionRepository;
    private final AiExtractionService aiExtractionService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ChatService(
            ChatHistoryRepository chatHistoryRepository,
            ChatDocumentRepository chatDocumentRepository,
            MongoExtractionRepository mongoExtractionRepository,
            AiExtractionService aiExtractionService
    ) {
        this.chatHistoryRepository = chatHistoryRepository;
        this.chatDocumentRepository = chatDocumentRepository;
        this.mongoExtractionRepository = mongoExtractionRepository;
        this.aiExtractionService = aiExtractionService;
    }

    public ChatResponse ask(
            Long chatSessionId,
            Long userId,
            String userMessage
    ) {

        if (userMessage == null || userMessage.isBlank()) {
            throw new RuntimeException("Chat message must not be empty.");
        }

        /*
         * Save User Message
         */
        chatHistoryRepository.save(
                ChatHistory.builder()
                        .chatSessionId(chatSessionId)
                        .userId(userId)
                        .role(ChatHistory.Role.USER)
                        .message(userMessage)
                        .build()
        );

        /*
         * Get all uploaded documents of this chat
         */
        List<ChatDocument> documents =
                chatDocumentRepository.findByChatSessionId(chatSessionId);

        if (documents.isEmpty()) {
            throw new RuntimeException(
                    "No documents found for this chat."
            );
        }

        StringBuilder contextBuilder = new StringBuilder();
        /*
         * Build context from all uploaded documents
         */
        for (ChatDocument chatDocument : documents) {

            MongoExtractionResult extraction =
                    mongoExtractionRepository
                            .findByDocumentId(chatDocument.getDocumentId())
                            .orElse(null);

            if (extraction == null) {
                continue;
            }

            contextBuilder.append("\n============================\n");
            contextBuilder.append("DOCUMENT ID : ")
                    .append(chatDocument.getDocumentId())
                    .append("\n");

            contextBuilder.append(
                    buildContext(extraction)
            );

            contextBuilder.append("\n");
        }

        /*
         * Previous Conversation
         */
        String history =
                buildHistoryTranscript(
                        chatSessionId,
                        userId
                );
        /*
         * Build AI Prompt
         */
        String prompt =
                "You are an AI assistant.\n"
                        + "Answer ONLY from the uploaded documents.\n"
                        + "If the answer is not present in any uploaded document, reply exactly:\n"
                        + "\"I couldn't find that information in the uploaded documents.\"\n\n"

                        + "DOCUMENT DATA:\n"
                        + contextBuilder

                        + "\n\n"

                        + (history.isBlank()
                        ? ""
                        : "PREVIOUS CONVERSATION:\n"
                          + history
                          + "\n\n")

                        + "USER QUESTION:\n"
                        + userMessage;

        /*
         * Ask Gemini
         */
        String reply =
                aiExtractionService.generateText(prompt);

        if (reply == null || reply.startsWith("Error")) {

            reply =
                    "Sorry, I could not process your request right now.";

        }

        /*
         * Save Assistant Reply
         */
        chatHistoryRepository.save(

                ChatHistory.builder()
                        .chatSessionId(chatSessionId)
                        .userId(userId)
                        .role(ChatHistory.Role.ASSISTANT)
                        .message(reply)
                        .build()

        );
        /*
         * Return Response
         */
        return ChatResponse.builder()
                .chatSessionId(chatSessionId)
                .reply(reply)
                .build();
    }

    public List<ChatHistory> getHistory(
            Long chatSessionId,
            Long userId
    ) {

        return chatHistoryRepository
                .findByChatSessionIdAndUserIdOrderByCreatedAtAsc(
                        chatSessionId,
                        userId
                );
    }

    private String buildContext(MongoExtractionResult extraction) {
        try {
            return objectMapper.writeValueAsString(extraction.getParsedFields());
        } catch (Exception e) {
            return extraction.getRawJsonData() != null ? extraction.getRawJsonData() : "{}";
        }
    }

    private String buildHistoryTranscript(
            Long chatSessionId,
            Long userId
    ) {

        List<ChatHistory> history =
                chatHistoryRepository
                        .findByChatSessionIdAndUserIdOrderByCreatedAtAsc(
                                chatSessionId,
                                userId
                        );

        int from = Math.max(
                0,
                history.size() - MAX_HISTORY_MESSAGES
        );

        return history.subList(from, history.size())
                .stream()
                .map(h -> h.getRole().name() + ": " + h.getMessage())
                .collect(Collectors.joining("\n"));
    }
}
