package com.pdf2data.platform.chat.repository;

import com.pdf2data.platform.chat.entity.ChatDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatDocumentRepository
        extends JpaRepository<ChatDocument, Long> {

    List<ChatDocument> findByChatSessionId(Long chatSessionId);

}