package com.insurewell.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Policy Data Transfer Object
 * Used for API request/response serialization.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyDTO {

  private String id;
  private String holderName;
  private String planName;
  private Double coverageAmount;
  private BigDecimal premiumAmount;
  private String currency;
  private String status;
  private String startDate;
  private String endDate;
  private String createdAt;

}
