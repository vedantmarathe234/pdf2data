package com.pdf2data.platform.chat.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Parent Chat Session
     */
    @Column(nullable = false)
    private Long chatSessionId;

    /**
     * Uploaded Document
     */
    @Column(nullable = false)
    private Long documentId;

    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
