package com.example.etl.dto;

public record IncidentExtraction(
    String orderCode,
    String licensePlate,
    String incidentType,
    String urgency
) {}