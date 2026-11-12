package com.insurewell.service;

import com.stripe.exception.SignatureVerificationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class StripeWebhookServiceTest {
  @Test
  void rejectsInvalidWebhookSignature() {
    StripeWebhookService service = new StripeWebhookService("whsec_test", mock(PaymentService.class));

    assertThrows(SignatureVerificationException.class,
      () -> service.handle("{\"id\":\"evt_1\",\"type\":\"checkout.session.completed\"}", "t=1,v1=invalid"));
  }
}
