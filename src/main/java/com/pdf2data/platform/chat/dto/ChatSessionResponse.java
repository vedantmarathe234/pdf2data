package com.pdf2data.platform.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatSessionResponse {

    private Long id;

    private String title;

    private String fileName;

    private Boolean pinned;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}