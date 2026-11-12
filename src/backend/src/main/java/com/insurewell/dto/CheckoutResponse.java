package com.insurewell.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CheckoutResponse {
  private String paymentId;
  private String checkoutUrl;
}
