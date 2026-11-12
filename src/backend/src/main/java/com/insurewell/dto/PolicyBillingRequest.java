package com.insurewell.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PolicyBillingRequest {
  private BigDecimal premiumAmount;
  private String currency;
  private String ownerPrincipal;
}
