package com.pdf2data.platform.document.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExtractionResponse {

    private Long documentId;
    private String fileName;
    private String documentType;
    private String status;
    private LocalDateTime extractedAt;
}