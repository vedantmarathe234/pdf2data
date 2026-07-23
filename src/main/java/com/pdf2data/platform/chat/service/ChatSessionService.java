package com.pdf2data.platform.chat.service;

import com.pdf2data.platform.auth.entity.User;
import com.pdf2data.platform.chat.dto.ChatDocumentResponse;
import com.pdf2data.platform.chat.dto.ChatSessionDetailsResponse;
import com.pdf2data.platform.chat.dto.ChatSessionResponse;
import com.pdf2data.platform.chat.entity.ChatDocument;
import com.pdf2data.platform.chat.entity.ChatHistory;
import com.pdf2data.platform.chat.entity.ChatSession;
import com.pdf2data.platform.chat.repository.ChatDocumentRepository;
import com.pdf2data.platform.chat.repository.ChatHistoryRepository;
import com.pdf2data.platform.chat.repository.ChatSessionRepository;
import com.pdf2data.platform.document.entity.Document;
import com.pdf2data.platform.document.repository.DocumentRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;


@Service
public class ChatSessionService {


    private final ChatSessionRepository chatSessionRepository;

    private final ChatDocumentRepository chatDocumentRepository;

    private final ChatHistoryRepository chatHistoryRepository;

    private final DocumentRepository documentRepository;



    public ChatSessionService(
            ChatSessionRepository chatSessionRepository,
            ChatDocumentRepository chatDocumentRepository,
            ChatHistoryRepository chatHistoryRepository,
            DocumentRepository documentRepository
    ) {

        this.chatSessionRepository = chatSessionRepository;
        this.chatDocumentRepository = chatDocumentRepository;
        this.chatHistoryRepository = chatHistoryRepository;
        this.documentRepository = documentRepository;

    }



    /*
     * Create New Chat
     */
    public ChatSession createChat(
            String fileName,
            User user
    ) {

        ChatSession session =
                ChatSession.builder()
                        .title(fileName)
                        .fileName(fileName)
                        .user(user)
                        .pinned(false)
                        .build();


        return chatSessionRepository.save(session);

    }




    /*
     * Get Session
     */
    public ChatSession getSession(Long id) {

        return chatSessionRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Chat session not found"
                        ));

    }




    /*
     * Sidebar History
     */
    public List<ChatSessionResponse> getUserChats(
            Long userId
    ) {


        return chatSessionRepository
                .findByUserIdOrderByPinnedDescUpdatedAtDesc(userId)

                .stream()

                .map(session ->
                        ChatSessionResponse.builder()

                                .id(session.getId())

                                .title(session.getTitle())

                                .fileName(session.getFileName())

                                .pinned(session.getPinned())

                                .createdAt(session.getCreatedAt())

                                .updatedAt(session.getUpdatedAt())

                                .build()
                )

                .collect(Collectors.toList());

    }





    /*
     * Open Old Chat
     */
    public ChatSessionDetailsResponse getSessionDetails(
            Long chatSessionId,
            Long userId
    ) {


        ChatSession session =
                getSession(chatSessionId);



        List<ChatDocumentResponse> documents =

                chatDocumentRepository
                        .findByChatSessionId(chatSessionId)

                        .stream()

                        .map(chatDocument -> {


                            Document document =
                                    documentRepository
                                            .findById(
                                                    chatDocument.getDocumentId()
                                            )
                                            .orElse(null);



                            return ChatDocumentResponse.builder()

                                    .documentId(
                                            chatDocument.getDocumentId()
                                    )

                                    .fileName(
                                            document != null
                                                    ? document.getFileName()
                                                    : ""
                                    )

                                    .documentType(
                                            document != null
                                                    ? document.getDocumentType()
                                                    : ""
                                    )

                                    .build();


                        })

                        .toList();



        List<ChatHistory> messages =

                chatHistoryRepository
                        .findByChatSessionIdAndUserIdOrderByCreatedAtAsc(
                                chatSessionId,
                                userId
                        );



        return ChatSessionDetailsResponse.builder()

                .session(
                        ChatSessionResponse.builder()

                                .id(session.getId())

                                .title(session.getTitle())

                                .fileName(session.getFileName())

                                .pinned(session.getPinned())

                                .createdAt(session.getCreatedAt())

                                .updatedAt(session.getUpdatedAt())

                                .build()
                )


                .documents(documents)

                .messages(messages)

                .build();

    }





    /*
     * Rename Chat
     */
    public void renameChat(
            Long id,
            String title
    ) {


        ChatSession session =
                getSession(id);


        session.setTitle(title);


        chatSessionRepository.save(session);

    }





    /*
     * Pin / Unpin Chat
     */
    public void togglePin(
            Long id
    ) {


        ChatSession session =
                getSession(id);


        session.setPinned(
                !session.getPinned()
        );


        chatSessionRepository.save(session);

    }





    /*
     * Delete Chat
     */
    @Transactional
    public void deleteChat(
            Long id
    ) {

        ChatSession session =
                getSession(id);

        // Delete all chat messages
        chatHistoryRepository.deleteByChatSessionId(id);

        // Delete chat-document mapping
        chatDocumentRepository.deleteAll(
                chatDocumentRepository.findByChatSessionId(id)
        );

        // Delete chat session
        chatSessionRepository.delete(session);

    }

}