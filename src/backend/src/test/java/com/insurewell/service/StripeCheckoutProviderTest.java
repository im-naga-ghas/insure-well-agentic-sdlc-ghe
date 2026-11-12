package com.insurewell.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;

class StripeCheckoutProviderTest {
  @Test
  void refusesProductionOrMissingStripeCredentials() {
    StripeCheckoutProvider missingKey = new StripeCheckoutProvider("", "http://localhost:3000");
    StripeCheckoutProvider liveKey = new StripeCheckoutProvider("sk" + "_live_not-a-real-key", "http://localhost:3000");

    assertThrows(CheckoutProviderException.class,
      () -> missingKey.createSession("payment", "policy", BigDecimal.ONE, "USD"));
    assertThrows(CheckoutProviderException.class,
      () -> liveKey.createSession("payment", "policy", BigDecimal.ONE, "USD"));
  }
}
