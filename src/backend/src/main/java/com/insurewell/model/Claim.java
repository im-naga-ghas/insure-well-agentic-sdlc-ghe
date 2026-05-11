package com.insurewell.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Claim Entity
 * Represents an insurance claim submitted by a policyholder.
 */
@Entity
@Table(name = "claims")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Claim {

  @Id
  private String id;

  @Column(nullable = false)
  private String policyId;

  @Column(nullable = false)
  private Double amount;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String description;

  @Column(nullable = false)
  private String status; // "Pending", "Approved", "Rejected"

  @Column
  private String fileName;

  @Column(nullable = false)
  private LocalDateTime submittedAt;

  @Column(nullable = false)
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    if (submittedAt == null) {
      submittedAt = LocalDateTime.now();
    }
    if (updatedAt == null) {
      updatedAt = LocalDateTime.now();
    }
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }

}
