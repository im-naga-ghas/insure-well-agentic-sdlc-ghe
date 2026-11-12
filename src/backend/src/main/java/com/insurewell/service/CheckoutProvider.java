package com.insurewell.service;

import java.math.BigDecimal;

public interface CheckoutProvider {
  CheckoutSession createSession(String paymentId, String policyId, BigDecimal amount, String currency);
}
