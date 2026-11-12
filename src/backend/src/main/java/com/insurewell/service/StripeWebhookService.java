package com.insurewell.service;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class StripeWebhookService {
  private final String webhookSecret;
  private final PaymentService paymentService;

  public StripeWebhookService(
      @Value("${stripe.webhook-secret:}") String webhookSecret,
      PaymentService paymentService) {
    this.webhookSecret = webhookSecret;
    this.paymentService = paymentService;
  }

  public void handle(String payload, String signature) throws SignatureVerificationException {
    if (webhookSecret == null || webhookSecret.isBlank()) {
      throw new SignatureVerificationException("Webhook secret is not configured", signature);
    }
    Event event = Webhook.constructEvent(payload, signature, webhookSecret);
    String type = event.getType();
    if (!type.startsWith("checkout.session.")) {
      return;
    }

    StripeObject object = event.getDataObjectDeserializer().getObject().orElse(null);
    if (!(object instanceof Session session)) {
      return;
    }
    paymentService.processStripeEvent(event.getId(), type, session.getId(),
      session.getPaymentIntent(), session.getPaymentStatus());
  }
}
