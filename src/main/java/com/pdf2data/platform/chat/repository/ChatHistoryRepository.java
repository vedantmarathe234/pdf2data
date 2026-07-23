package com.pdf2data.platform.chat.repository;

import com.pdf2data.platform.chat.entity.ChatHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatHistoryRepository extends JpaRepository<ChatHistory, Long> {

    List<ChatHistory> findByChatSessionIdAndUserIdOrderByCreatedAtAsc(
            Long chatSessionId,
            Long userId
    );

    void deleteByChatSessionId(Long chatSessionId);
}