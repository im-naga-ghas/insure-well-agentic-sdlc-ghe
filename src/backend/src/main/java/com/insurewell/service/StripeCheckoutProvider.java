package com.insurewell.service;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

@Component
public class StripeCheckoutProvider implements CheckoutProvider {
  private final String secretKey;
  private final String frontendBaseUrl;

  public StripeCheckoutProvider(
      @Value("${stripe.secret-key:}") String secretKey,
      @Value("${stripe.frontend-base-url:http://localhost:3000}") String frontendBaseUrl) {
    this.secretKey = secretKey;
    this.frontendBaseUrl = frontendBaseUrl.replaceAll("/+$", "");
  }

  @Override
  public CheckoutSession createSession(String paymentId, String policyId, BigDecimal amount, String currency) {
    if (secretKey == null || !secretKey.startsWith("sk_test_")) {
      throw new CheckoutProviderException("Stripe test-mode credentials are not configured");
    }

    long amountInCents;
    try {
      amountInCents = amount.movePointRight(2).longValueExact();
    } catch (ArithmeticException ex) {
      throw new CheckoutProviderException("Premium amount must be in whole cents");
    }

    SessionCreateParams params = SessionCreateParams.builder()
      .setMode(SessionCreateParams.Mode.PAYMENT)
      .setSuccessUrl(frontendBaseUrl + "/?payment=success&policyId="
        + UriUtils.encodeQueryParam(policyId, StandardCharsets.UTF_8)
        + "&session_id={CHECKOUT_SESSION_ID}")
      .setCancelUrl(frontendBaseUrl + "/?payment=cancelled&policyId="
        + UriUtils.encodeQueryParam(policyId, StandardCharsets.UTF_8))
      .putMetadata("paymentId", paymentId)
      .putMetadata("policyId", policyId)
      .addLineItem(SessionCreateParams.LineItem.builder()
        .setQuantity(1L)
        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
          .setCurrency(currency.toLowerCase())
          .setUnitAmount(amountInCents)
          .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
            .setName("Policy premium " + policyId)
            .build())
          .build())
        .build())
      .build();

    try {
      Session session = Session.create(params, RequestOptions.builder()
        .setApiKey(secretKey)
        .setIdempotencyKey(paymentId)
        .build());
      if (session.getUrl() == null) {
        throw new CheckoutProviderException("Stripe did not return a Checkout URL");
      }
      return new CheckoutSession(session.getId(), session.getUrl());
    } catch (StripeException ex) {
      throw new CheckoutProviderException("Stripe Checkout could not be created");
    }
  }
}
