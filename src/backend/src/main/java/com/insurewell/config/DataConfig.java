package com.insurewell.config;

import com.insurewell.model.Claim;
import com.insurewell.model.Policy;
import com.insurewell.repository.ClaimRepository;
import com.insurewell.repository.PolicyRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Database Configuration & Seed Data
 * Auto-seeds the database with sample policies and claims on application startup.
 */
@Configuration
public class DataConfig {

  private static String toIsoString(LocalDateTime dt) {
    return dt.format(DateTimeFormatter.ISO_DATE_TIME) + "Z";
  }

  private static LocalDateTime parseIsoString(String s) {
    return LocalDateTime.parse(s.replace("Z", ""), DateTimeFormatter.ISO_DATE_TIME);
  }

  @Bean
  public CommandLineRunner loadData(PolicyRepository policyRepo, ClaimRepository claimRepo) {
    return args -> {
      // Only seed if empty
      if (policyRepo.count() == 0) {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));

        // Create seed policies
        List<Policy> policies = List.of(
          Policy.builder()
            .id("POL-2024-001")
            .holderName("Alex Johnson")
            .planName("InsureWell Premium Health Plan")
            .coverageAmount(250000.0)
            .status("active")
            .startDate("2024-01-01")
            .endDate(LocalDate.now(ZoneId.of("UTC")).plusDays(15).toString())
            .createdAt(now)
            .build(),
          Policy.builder()
            .id("POL-2024-002")
            .holderName("Maria Garcia")
            .planName("InsureWell Essential Care Plan")
            .coverageAmount(150000.0)
            .status("active")
            .startDate("2024-02-15")
            .endDate("2027-02-14")
            .createdAt(now)
            .build(),
          Policy.builder()
            .id("POL-2023-009")
            .holderName("David Chen")
            .planName("InsureWell Family Plus Plan")
            .coverageAmount(500000.0)
            .status("inactive")
            .startDate("2023-06-01")
            .endDate("2024-05-31")
            .createdAt(now)
            .build()
        );
        policyRepo.saveAll(policies);

        // Create seed claims
        LocalDateTime t1 = parseIsoString("2024-05-15T14:30:00.000Z");
        LocalDateTime t2 = parseIsoString("2024-05-20T10:15:00.000Z");
        LocalDateTime t3 = parseIsoString("2024-06-01T09:45:00.000Z");
        LocalDateTime t4 = parseIsoString("2024-06-10T16:20:00.000Z");
        LocalDateTime t5 = parseIsoString("2024-07-05T11:00:00.000Z");
        LocalDateTime t6 = parseIsoString("2024-07-10T09:00:00.000Z");
        LocalDateTime t7 = parseIsoString("2024-07-15T13:30:00.000Z");

        List<Claim> claims = List.of(
          Claim.builder()
            .id("CLM-1715787000000")
            .policyId("POL-2024-001")
            .amount(1500.0)
            .description("Doctor visit for annual checkup")
            .status("Approved")
            .fileName(null)
            .submittedAt(t1)
            .updatedAt(t1)
            .build(),
          Claim.builder()
            .id("CLM-1716216600000")
            .policyId("POL-2024-001")
            .amount(850.0)
            .description("Lab tests and blood work")
            .status("Approved")
            .fileName("lab_results.pdf")
            .submittedAt(t2)
            .updatedAt(t2)
            .build(),
          Claim.builder()
            .id("CLM-1717264800000")
            .policyId("POL-2024-002")
            .amount(2200.0)
            .description("Emergency room visit")
            .status("Pending")
            .fileName(null)
            .submittedAt(t3)
            .updatedAt(t3)
            .build(),
          Claim.builder()
            .id("CLM-1717955400000")
            .policyId("POL-2024-002")
            .amount(500.0)
            .description("Prescription medications")
            .status("Approved")
            .fileName("pharmacy_receipt.jpg")
            .submittedAt(t4)
            .updatedAt(t4)
            .build(),
          Claim.builder()
            .id("CLM-1720170000000")
            .policyId("POL-2024-001")
            .amount(3000.0)
            .description("Physical therapy sessions (10 sessions)")
            .status("Pending")
            .fileName(null)
            .submittedAt(t5)
            .updatedAt(t5)
            .build(),
          Claim.builder()
            .id("CLM-1720598400000")
            .policyId("POL-2024-002")
            .amount(1200.0)
            .description("Dental cleaning and checkup")
            .status("Rejected")
            .fileName("dental_receipt.pdf")
            .submittedAt(t6)
            .updatedAt(t6)
            .build(),
          Claim.builder()
            .id("CLM-1721041800000")
            .policyId("POL-2024-001")
            .amount(750.0)
            .description("X-ray imaging for dental work")
            .status("Pending")
            .fileName("dental_xray.png")
            .submittedAt(t7)
            .updatedAt(t7)
            .build()
        );
        claimRepo.saveAll(claims);
      }
    };
  }

}
