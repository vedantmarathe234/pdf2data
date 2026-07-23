package com.pdf2data.platform.document.service;

import com.pdf2data.platform.document.dto.ExtractionResponse;
import com.pdf2data.platform.document.entity.Document;
import com.pdf2data.platform.document.entity.MongoExtractionResult;
import com.pdf2data.platform.document.repository.DocumentRepository;
import com.pdf2data.platform.document.repository.MongoExtractionRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ExtractionHistoryService {

    private final DocumentRepository documentRepository;
    private final MongoExtractionRepository mongoRepository;

    public ExtractionHistoryService(DocumentRepository documentRepository,
                                    MongoExtractionRepository mongoRepository) {
        this.documentRepository = documentRepository;
        this.mongoRepository = mongoRepository;
    }

    public List<ExtractionResponse> getAllExtractions() {

        List<Document> documents = documentRepository.findAll();

        List<ExtractionResponse> response = new ArrayList<>();

        for (Document doc : documents) {

            MongoExtractionResult mongo =
                    mongoRepository.findByDocumentId(doc.getId()).orElse(null);

            response.add(
                    ExtractionResponse.builder()
                            .documentId(doc.getId())
                            .fileName(doc.getFileName())
                            .documentType(doc.getDocumentType())
                            .status(mongo != null ? "Success" : "Pending")
                            .extractedAt(
                                    mongo != null
                                            ? mongo.getProcessedAt()
                                            : doc.getUploadDate()
                            )
                            .build()
            );
        }

        return response;
    }
}