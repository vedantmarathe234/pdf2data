package com.pdf2data.platform.chat.repository;

import com.pdf2data.platform.chat.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    List<ChatSession> findByUserIdOrderByPinnedDescUpdatedAtDesc(Long userId);

}