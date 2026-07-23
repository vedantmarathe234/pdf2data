package com.pdf2data.platform.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatDocumentResponse {

    private Long documentId;

    private String fileName;

    private String documentType;

}
