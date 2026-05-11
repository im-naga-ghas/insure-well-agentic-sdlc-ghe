package com.insurewell.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Claim Data Transfer Object
 * Used for API request/response serialization.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClaimDTO {

  private String id;
  private String policyId;
  private Double amount;
  private String description;
  private String status;
  private String fileName;
  private String submittedAt;
  private String updatedAt;

}
