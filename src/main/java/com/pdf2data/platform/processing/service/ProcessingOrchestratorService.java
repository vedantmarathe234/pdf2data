package com.pdf2data.platform.processing.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pdf2data.platform.auth.entity.User;
import com.pdf2data.platform.chat.entity.ChatDocument;
import com.pdf2data.platform.chat.entity.ChatSession;
import com.pdf2data.platform.chat.repository.ChatDocumentRepository;
import com.pdf2data.platform.chat.service.ChatSessionService;
import com.pdf2data.platform.classification.dto.ClassificationResult;
import com.pdf2data.platform.classification.service.DocumentClassifierService;
import com.pdf2data.platform.confidence.dto.ConfidenceResult;
import com.pdf2data.platform.confidence.service.ConfidenceService;
import com.pdf2data.platform.document.entity.Document;
import com.pdf2data.platform.document.service.AiExtractionService;
import com.pdf2data.platform.document.service.DocumentStorageService;
import com.pdf2data.platform.document.service.ExtractionService;
import com.pdf2data.platform.document.service.FileStorageService;
import com.pdf2data.platform.document.service.ImageOcrProcessor;
import com.pdf2data.platform.highlight.dto.HighlightRegion;
import com.pdf2data.platform.highlight.service.HighlightService;
import com.pdf2data.platform.learning.service.LearningService;
import com.pdf2data.platform.processing.dto.ProcessingResponse;
import com.pdf2data.platform.processing.entity.ExtractionLog;
import com.pdf2data.platform.processing.repository.ExtractionLogRepository;
import com.pdf2data.platform.reasoning.dto.ReasoningResult;
import com.pdf2data.platform.reasoning.service.ReasoningService;
import com.pdf2data.platform.validation.dto.ValidationResult;
import com.pdf2data.platform.validation.service.ValidationService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ProcessingOrchestratorService {

    private static final Logger log =
            LoggerFactory.getLogger(ProcessingOrchestratorService.class);

    private final FileStorageService fileStorageService;
    private final ExtractionService extractionService;
    private final ImageOcrProcessor imageOcrProcessor;
    private final AiExtractionService aiExtractionService;
    private final DocumentClassifierService documentClassifierService;
    private final ValidationService validationService;
    private final ConfidenceService confidenceService;
    private final HighlightService highlightService;
    private final LearningService learningService;
    private final ReasoningService reasoningService;
    private final DocumentStorageService documentStorageService;
    private final ExtractionLogRepository extractionLogRepository;

    // NEW
    private final ChatSessionService chatSessionService;
    private final ChatDocumentRepository chatDocumentRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ProcessingOrchestratorService(
            FileStorageService fileStorageService,
            ExtractionService extractionService,
            ImageOcrProcessor imageOcrProcessor,
            AiExtractionService aiExtractionService,
            DocumentClassifierService documentClassifierService,
            ValidationService validationService,
            ConfidenceService confidenceService,
            HighlightService highlightService,
            LearningService learningService,
            ReasoningService reasoningService,
            DocumentStorageService documentStorageService,
            ExtractionLogRepository extractionLogRepository,
            ChatSessionService chatSessionService,
            ChatDocumentRepository chatDocumentRepository
    ) {

        this.fileStorageService = fileStorageService;
        this.extractionService = extractionService;
        this.imageOcrProcessor = imageOcrProcessor;
        this.aiExtractionService = aiExtractionService;
        this.documentClassifierService = documentClassifierService;
        this.validationService = validationService;
        this.confidenceService = confidenceService;
        this.highlightService = highlightService;
        this.learningService = learningService;
        this.reasoningService = reasoningService;
        this.documentStorageService = documentStorageService;
        this.extractionLogRepository = extractionLogRepository;

        // NEW
        this.chatSessionService = chatSessionService;
        this.chatDocumentRepository = chatDocumentRepository;
    }
    public ProcessingResponse process(
            MultipartFile file,
            String prompt,
            Long chatSessionId,
            User user
    ) {

        String fileName = Objects.requireNonNull(file.getOriginalFilename());

        /*
         * Save uploaded file
         */
        String filePath = fileStorageService.saveFile(file);

        /*
         * Create Document object
         */
        Document document = Document.builder()
                .fileName(fileName)
                .filePath(filePath)
                .user(user)
                .build();

        /*
         * Create New Chat OR Open Existing Chat
         */
        ChatSession session;

        if (chatSessionId == null) {

            session = chatSessionService.createChat(fileName, user);

        } else {

            session = chatSessionService.getSession(chatSessionId);

        }

        boolean isPdf = fileName.toLowerCase().endsWith(".pdf");

        List<ExtractionLog> pendingLogs = new ArrayList<>();

        List<String> pages = new ArrayList<>();

        String rawText;

        long t0 = System.currentTimeMillis();

        try {

            if (isPdf) {

                pages = extractionService.extractTextPerPageFromPDF(filePath);
                rawText = String.join("\n", pages);

            } else {

                rawText = imageOcrProcessor.extractTextFromImage(filePath);
                pages.add(rawText);

            }

            bufferLog(
                    pendingLogs,
                    ExtractionLog.Stage.EXTRACTION,
                    ExtractionLog.Status.SUCCESS,
                    "Extracted "
                            + (rawText == null ? 0 : rawText.length())
                            + " characters",
                    System.currentTimeMillis() - t0
            );

        } catch (Exception e) {

            bufferLog(
                    pendingLogs,
                    ExtractionLog.Stage.EXTRACTION,
                    ExtractionLog.Status.FAILED,
                    e.getMessage(),
                    System.currentTimeMillis() - t0
            );

            throw e;
        }

        if (rawText == null || rawText.isBlank()) {
            throw new RuntimeException(
                    "No text could be extracted from document : " + fileName
            );
        }

        /*
         * Document Classification
         */
        t0 = System.currentTimeMillis();

        ClassificationResult classification =
                documentClassifierService.classify(rawText);

        bufferLog(
                pendingLogs,
                ExtractionLog.Stage.CLASSIFICATION,
                ExtractionLog.Status.SUCCESS,
                "Classified as " + classification.getDocumentType(),
                System.currentTimeMillis() - t0
        );
        /*
         * AI Extraction
         */
        t0 = System.currentTimeMillis();

        String strictPrompt = (prompt == null ? "" : prompt)
                + "\n\n"
                + "CRITICAL STRUCTURAL MANDATES:\n"
                + "1. Return ONLY valid JSON.\n"
                + "2. No markdown.\n"
                + "3. No explanation.\n"
                + "4. Missing fields must be null.";

        String rawAiResponse =
                aiExtractionService.extractDataWithAI(rawText, strictPrompt);

        if (rawAiResponse == null || rawAiResponse.startsWith("Error")) {

            bufferLog(
                    pendingLogs,
                    ExtractionLog.Stage.AI_EXTRACTION,
                    ExtractionLog.Status.FAILED,
                    rawAiResponse,
                    System.currentTimeMillis() - t0
            );

            throw new RuntimeException(
                    "AI Processing Failed : " + rawAiResponse
            );
        }

        Map<String, Object> parsedFields;

        try {

            parsedFields = objectMapper.readValue(
                    sanitizeJson(rawAiResponse),
                    new TypeReference<Map<String, Object>>() {
                    });

        } catch (Exception e) {

            log.warn(
                    "AI returned invalid JSON : {}",
                    e.getMessage()
            );

            parsedFields = new HashMap<>();
            parsedFields.put("rawOutputText", rawAiResponse);
        }

        bufferLog(
                pendingLogs,
                ExtractionLog.Stage.AI_EXTRACTION,
                ExtractionLog.Status.SUCCESS,
                "Extracted " + parsedFields.size() + " fields",
                System.currentTimeMillis() - t0
        );

        /*
         * Validation
         */
        ValidationResult validationResult =
                validationService.validate(parsedFields);

        /*
         * Highlights
         */
        List<HighlightRegion> highlights =
                highlightService.mapHighlights(
                        parsedFields,
                        rawText,
                        pages
                );

        /*
         * Save Document
         */
        document.setDocumentType(classification.getDocumentType());

        t0 = System.currentTimeMillis();

        documentStorageService.saveAll(
                document,
                rawAiResponse,
                parsedFields
        );

        /*
         * Link document with chat session
         */
        ChatDocument chatDocument = ChatDocument.builder()
                .chatSessionId(session.getId())
                .documentId(document.getId())
                .build();

        chatDocumentRepository.save(chatDocument);

        bufferLog(
                pendingLogs,
                ExtractionLog.Stage.SAVE,
                ExtractionLog.Status.SUCCESS,
                "Persisted document + extraction results",
                System.currentTimeMillis() - t0
        );

        flushLogs(
                pendingLogs,
                document.getId()
        );

        ConfidenceResult confidenceResult =
                confidenceService.computeConfidence(
                        document.getId(),
                        parsedFields,
                        validationResult,
                        highlights
                );
        /*
         * Learning
         */
        try {

            learningService.learnFromExtraction(
                    classification.getDocumentType(),
                    rawText,
                    highlights
            );

        } catch (Exception e) {

            log.warn(
                    "Learning stage failed non-fatally: {}",
                    e.getMessage()
            );

        }

        /*
         * Reasoning
         */
        ReasoningResult reasoning =
                reasoningService.buildReasoning(
                        classification,
                        validationResult,
                        confidenceResult
                );

        /*
         * Final Response
         */
        return ProcessingResponse.builder()
                .chatSessionId(session.getId())
                .documentId(document.getId())
                .fileName(fileName)
                .documentType(classification.getDocumentType())
                .documentTypeConfidence(classification.getConfidence())
                .data(parsedFields)
                .fieldConfidence(confidenceResult.getFieldConfidence())
                .overallConfidence(confidenceResult.getOverallConfidence())
                .validationIssues(validationResult.getIssues())
                .highlights(highlights)
                .reasoning(reasoning)
                .build();
    }

    private String sanitizeJson(String json) {


        if (json == null) {
            return "{}";
        }

        String cleaned = json.trim();

        if (cleaned.startsWith("```")) {

            int firstNewline = cleaned.indexOf('\n');

            cleaned = firstNewline != -1
                    ? cleaned.substring(firstNewline + 1)
                    : cleaned.substring(3);
        }

        if (cleaned.endsWith("```")) {

            cleaned = cleaned.substring(
                    0,
                    cleaned.length() - 3
            );
        }

        cleaned = cleaned.trim();

        int start = indexOfFirst(cleaned, '{', '[');
        int end = indexOfLast(cleaned, '}', ']');

        if (start != -1 && end != -1 && end > start) {

            cleaned = cleaned.substring(start, end + 1);

        }

        return cleaned.trim();
    }

    private int indexOfFirst(String text, char a, char b) {

        int ia = text.indexOf(a);
        int ib = text.indexOf(b);

        if (ia == -1) {
            return ib;
        }

        if (ib == -1) {
            return ia;
        }

        return Math.min(ia, ib);
    }

    private int indexOfLast(String text, char a, char b) {

        int ia = text.lastIndexOf(a);
        int ib = text.lastIndexOf(b);

        return Math.max(ia, ib);
    }

    private void bufferLog(
            List<ExtractionLog> pendingLogs,
            ExtractionLog.Stage stage,
            ExtractionLog.Status status,
            String message,
            long durationMs
    ) {

        pendingLogs.add(

                ExtractionLog.builder()
                        .stage(stage)
                        .status(status)
                        .message(message)
                        .durationMs(durationMs)
                        .build()

        );
    }

    private void flushLogs(
            List<ExtractionLog> pendingLogs,
            Long documentId
    ) {

        try {

            for (ExtractionLog logEntry : pendingLogs) {

                logEntry.setDocumentId(documentId);

            }

            extractionLogRepository.saveAll(pendingLogs);

        } catch (Exception e) {

            log.warn(
                    "Failed to persist extraction logs : {}",
                    e.getMessage()
            );

        }
    }
}