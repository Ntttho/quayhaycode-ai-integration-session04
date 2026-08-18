package com.example.crm;

import com.example.crm.dto.IncidentExtraction;
import com.example.crm.entity.IncidentReport;
import com.example.crm.service.IncidentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class CrmApplication {

    private static final Logger log = LoggerFactory.getLogger(CrmApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(CrmApplication.class, args);
    }

    @Bean
    public CommandLineRunner demoRunner(IncidentService incidentService) {
        return args -> {
            log.info("========== RUNNING DEFENSIVE DATA EXTRACTION SIMULATION ==========");

            // Case 1: Healthy raw LLM data extraction
            IncidentExtraction case1 = new IncidentExtraction(
                "Nguyen Van A ", 
                " 29A-12345", 
                "Hanoi Highway Km12", 
                "High", 
                "Engine issues and minor smoke", 
                "2023-10-27T10:15:30"
            );
            log.info("Simulated LLM Output Case 1 (Raw DTO): {}", case1);
            IncidentReport report1 = incidentService.processAndSaveExtraction(case1);
            log.info("Saved Entity details: ID={}, Driver={}, Plate={}, Severity={}", 
                report1.getId(), report1.getDriverName(), report1.getLicensePlate(), report1.getSeverityLevel());

            // Case 2: Unstable raw LLM data with missing or corrupted fields (Defensive flow demonstration)
            IncidentExtraction case2 = new IncidentExtraction(
                "Tran Van B",
                "51G-99999",
                "Ho Chi Minh Highway",
                "Medium",
                "Flat tire",
                "INVALID_TIME_FORMAT_FROM_LLM" // Broken date
            );
            log.info("Simulated LLM Output Case 2 (Corrupted date): {}", case2);
            IncidentReport report2 = incidentService.processAndSaveExtraction(case2);
            log.info("Saved Entity details (Recovered from error): ID={}, DateFallback={}", 
                report2.getId(), report2.getEventTime());

            log.info("================== SIMULATION COMPLETE ==================");
        };
    }
}