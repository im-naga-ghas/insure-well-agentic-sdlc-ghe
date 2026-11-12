package com.insurewell.service;

import com.stripe.exception.SignatureVerificationException;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class StripeWebhookServiceTest {
  private static final String SECRET = "local-webhook-secret";

  @Test
  void rejectsInvalidWebhookSignature() {
    StripeWebhookService service = new StripeWebhookService(SECRET, mock(PaymentService.class));

    assertThrows(SignatureVerificationException.class,
      () -> service.handle("{\"id\":\"evt_1\",\"type\":\"checkout.session.completed\"}", "t=1,v1=invalid"));
  }

  @Test
  void verifiesSignatureBeforeProcessingCheckoutSuccess() throws Exception {
    PaymentService paymentService = mock(PaymentService.class);
    StripeWebhookService service = new StripeWebhookService(SECRET, paymentService);
    String payload = """
      {"id":"evt_1","object":"event","api_version":"2025-03-31.basil","created":1700000000,
       "data":{"object":{"id":"cs_test_123","object":"checkout.session","payment_intent":"pi_1","payment_status":"paid"}},
       "livemode":false,"pending_webhooks":1,"type":"checkout.session.completed"}
      """;
    long timestamp = Instant.now().getEpochSecond();
    String signedContent = timestamp + "." + payload;
    Mac hmac = Mac.getInstance("HmacSHA256");
    hmac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    String signature = "t=" + timestamp + ",v1="
      + HexFormat.of().formatHex(hmac.doFinal(signedContent.getBytes(StandardCharsets.UTF_8)));

    service.handle(payload, signature);

    verify(paymentService).processStripeEvent("evt_1", "checkout.session.completed",
      "cs_test_123", "pi_1", "paid");
  }
}
