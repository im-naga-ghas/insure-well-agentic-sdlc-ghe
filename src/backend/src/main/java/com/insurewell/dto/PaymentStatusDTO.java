package com.insurewell.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class PaymentStatusDTO {
  private String id;
  private BigDecimal amount;
  private String currency;
  private String status;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
