package com.example.etl.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "incident_reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncidentReport {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_code", nullable = false)
    private String orderCode;

    @Column(name = "license_plate", nullable = false)
    private String licensePlate;

    @Column(name = "incident_type", nullable = false)
    private String incidentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "urgency", nullable = false)
    private UrgencyLevel urgency;
}