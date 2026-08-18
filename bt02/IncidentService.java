package com.example.crm.service;

import com.example.crm.dto.IncidentExtraction;
import com.example.crm.entity.IncidentReport;
import com.example.crm.repository.IncidentReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

@Service
public class IncidentService {

    private static final Logger log = LoggerFactory.getLogger(IncidentService.class);
    private final IncidentReportRepository repository;

    public IncidentService(IncidentReportRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public IncidentReport processAndSaveExtraction(IncidentExtraction extraction) {
        log.info("Processing incident extraction: {}", extraction);

        // Defensive Guard: Check if extraction has absolute minimum values
        if (extraction.driverName() == null || extraction.driverName().isBlank()) {
            throw new IllegalArgumentException("Defensive Check Failed: Driver name extracted is null or empty");
        }

        // Safe Parsing of DateTime from LLM output
        LocalDateTime parsedTime;
        try {
            if (extraction.eventTimeRaw() != null && !extraction.eventTimeRaw().isBlank()) {
                parsedTime = LocalDateTime.parse(extraction.eventTimeRaw());
            } else {
                parsedTime = LocalDateTime.now(); // Fallback
            }
        } catch (DateTimeParseException e) {
            log.warn("Invalid time format provided by LLM: '{}'. Defaulting to current system time.", extraction.eventTimeRaw());
            parsedTime = LocalDateTime.now();
        }

        // Defensive mapping via custom Builder
        IncidentReport report = new IncidentReport.Builder()
                .driverName(extraction.driverName())
                .licensePlate(extraction.licensePlate())
                .location(extraction.location())
                .severityLevel(extraction.severityLevel())
                .description(extraction.description())
                .eventTime(parsedTime)
                .build();

        IncidentReport saved = repository.save(report);
        log.info("Incident successfully validated, mapped, and persistent with ID: {}", saved.getId());
        return saved;
    }
}