package com.pdf2data.platform.chat.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecentChatResponse {

    private Long chatId;

    private String title;

    private String fileName;

    private Boolean pinned;

    private Long documentId;

}
