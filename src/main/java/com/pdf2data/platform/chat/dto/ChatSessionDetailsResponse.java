package com.pdf2data.platform.chat.dto;

import com.pdf2data.platform.chat.entity.ChatHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatSessionDetailsResponse {

    private ChatSessionResponse session;

    private List<ChatDocumentResponse> documents;

    private List<ChatHistory> messages;

}
