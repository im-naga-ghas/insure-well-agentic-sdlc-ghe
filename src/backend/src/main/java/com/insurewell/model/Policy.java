package com.insurewell.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Policy Entity
 * Represents an insurance policy.
 */
@Entity
@Table(name = "policies")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Policy {

  @Id
  private String id;

  @Column(nullable = false)
  private String holderName;

  @Column(nullable = false)
  private String planName;

  @Column(nullable = false)
  private Double coverageAmount;

  @Column(nullable = false)
  private String status; // "active" or "inactive"

  @Column(precision = 12, scale = 2)
  private BigDecimal premiumAmount;

  @Column(nullable = false, length = 3)
  private String currency;

  @Column(length = 255)
  private String ownerPrincipal;

  @Column(nullable = false)
  private String startDate;

  @Column(nullable = false)
  private String endDate;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    if (createdAt == null) {
      createdAt = LocalDateTime.now();
    }
    if (currency == null) {
      currency = "USD";
    }
  }

}
