package com.example.crm.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * JPA Entity guarded against unsafe mutations. Maintains internal database consistency.
 */
@Entity
@Table(name = "incident_reports")
public class IncidentReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "driver_name", nullable = false)
    private String driverName;

    @Column(name = "license_plate", nullable = false, length = 20)
    private String licensePlate;

    @Column(name = "location", nullable = false)
    private String location;

    @Column(name = "severity_level", nullable = false, length = 20)
    private String severityLevel;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "event_time", nullable = false)
    private LocalDateTime eventTime;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Required by JPA Spec
    protected IncidentReport() {}

    // Private constructor enforcing creation only through safe builder or factory
    private IncidentReport(Builder builder) {
        this.driverName = builder.driverName;
        this.licensePlate = builder.licensePlate;
        this.location = builder.location;
        this.severityLevel = builder.severityLevel;
        this.description = builder.description;
        this.eventTime = builder.eventTime;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getDriverName() {
        return driverName;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public String getLocation() {
        return location;
    }

    public String getSeverityLevel() {
        return severityLevel;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getEventTime() {
        return eventTime;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // Fluent Defensive Builder
    public static class Builder {
        private String driverName;
        private String licensePlate;
        private String location;
        private String severityLevel;
        private String description;
        private LocalDateTime eventTime;

        public Builder driverName(String driverName) {
            if (driverName == null || driverName.isBlank()) {
                throw new IllegalArgumentException("Driver name cannot be empty");
            }
            this.driverName = driverName;
            return this;
        }

        public Builder licensePlate(String licensePlate) {
            if (licensePlate == null || licensePlate.isBlank()) {
                throw new IllegalArgumentException("License plate cannot be empty");
            }
            this.licensePlate = licensePlate;
            return this;
        }

        public Builder location(String location) {
            if (location == null || location.isBlank()) {
                throw new IllegalArgumentException("Location cannot be empty");
            }
            this.location = location;
            return this;
        }

        public Builder severityLevel(String severityLevel) {
            if (severityLevel == null || severityLevel.isBlank()) {
                this.severityLevel = "LOW";
            } else {
                this.severityLevel = severityLevel;
            }
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder eventTime(LocalDateTime eventTime) {
            this.eventTime = (eventTime != null) ? eventTime : LocalDateTime.now();
            return this;
        }

        public IncidentReport build() {
            // Defensive validation before instantiation
            if (driverName == null || licensePlate == null || location == null) {
                throw new IllegalStateException("Required fields must be resolved before mapping to DB Entity");
            }
            return new IncidentReport(this);
        }
    }
}