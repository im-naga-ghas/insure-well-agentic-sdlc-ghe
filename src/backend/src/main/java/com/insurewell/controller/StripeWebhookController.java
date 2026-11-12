package com.insurewell.controller;

import com.stripe.exception.SignatureVerificationException;
import com.insurewell.service.StripeWebhookService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks/stripe")
public class StripeWebhookController {
  private final StripeWebhookService webhookService;

  public StripeWebhookController(StripeWebhookService webhookService) {
    this.webhookService = webhookService;
  }

  @PostMapping
  public ResponseEntity<Void> receive(
      @RequestBody String payload,
      @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
    try {
      webhookService.handle(payload, signature);
      return ResponseEntity.ok().build();
    } catch (SignatureVerificationException | IllegalArgumentException ex) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
  }
}
