package com.example.crm.dto;

import java.time.LocalDateTime;

/**
 * Java Record DTO representing the raw extracted incident information from LLM.
 * Uses immutable representation to enforce safety and prevent modification of raw parsed data.
 */
public record IncidentExtraction(
    String driverName,
    String licensePlate,
    String location,
    String severityLevel,
    String description,
    String eventTimeRaw
) {
    // Canonical constructor with defensive checks and normalizations
    public IncidentExtraction {
        // Normalize empty fields to avoid unexpected whitespace issues
        driverName = (driverName != null) ? driverName.trim() : null;
        licensePlate = (licensePlate != null) ? licensePlate.trim().toUpperCase() : null;
        location = (location != null) ? location.trim() : null;
        severityLevel = (severityLevel != null) ? severityLevel.trim().toUpperCase() : "UNKNOWN";
        description = (description != null) ? description.trim() : null;
        eventTimeRaw = (eventTimeRaw != null) ? eventTimeRaw.trim() : null;
    }
}