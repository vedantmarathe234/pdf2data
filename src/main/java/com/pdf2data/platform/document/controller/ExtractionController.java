package com.pdf2data.platform.document.controller;

import com.pdf2data.platform.document.dto.ExtractionResponse;
import com.pdf2data.platform.document.service.ExtractionHistoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/extractions")
@CrossOrigin(origins = "*")
public class ExtractionController {

    private final ExtractionHistoryService extractionHistoryService;

    public ExtractionController(ExtractionHistoryService extractionHistoryService) {
        this.extractionHistoryService = extractionHistoryService;
    }

    @GetMapping
    public List<ExtractionResponse> getAllExtractions() {
        return extractionHistoryService.getAllExtractions();
    }
}