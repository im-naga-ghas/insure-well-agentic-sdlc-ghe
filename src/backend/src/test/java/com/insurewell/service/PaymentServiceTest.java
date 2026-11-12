package com.insurewell.service;

import com.insurewell.dto.CheckoutResponse;
import com.insurewell.model.PaymentStatus;
import com.insurewell.model.Policy;
import com.insurewell.model.PolicyPayment;
import com.insurewell.repository.PolicyPaymentRepository;
import com.insurewell.repository.PolicyRepository;
import com.insurewell.repository.ProcessedStripeEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {
  @Mock private PolicyRepository policyRepository;
  @Mock private PolicyPaymentRepository paymentRepository;
  @Mock private ProcessedStripeEventRepository eventRepository;
  @Mock private CheckoutProvider checkoutProvider;
  @InjectMocks private PaymentService paymentService;

  private Policy policy;
  private final Principal owner = () -> "holder@example.com";

  @BeforeEach
  void setUp() {
    policy = Policy.builder()
      .id("POL-1")
      .ownerPrincipal("holder@example.com")
      .premiumAmount(new BigDecimal("125.50"))
      .currency("USD")
      .status("active")
      .build();
  }

  private void stubPolicy() {
    when(policyRepository.findByIdForPaymentLock("POL-1")).thenReturn(Optional.of(policy));
  }

  @Test
  void checkoutUsesStoredPremiumForPolicyOwner() {
    stubPolicy();
    when(checkoutProvider.createSession(any(), eq("POL-1"), eq(new BigDecimal("125.50")), eq("USD")))
      .thenReturn(new CheckoutSession("cs_test_123", "https://checkout.stripe.com/session"));

    CheckoutResponse response = paymentService.startCheckout("POL-1", owner);

    assertEquals("https://checkout.stripe.com/session", response.getCheckoutUrl());
    verify(paymentRepository).saveAndFlush(any(PolicyPayment.class));
    verify(checkoutProvider).createSession(any(), eq("POL-1"), eq(new BigDecimal("125.50")), eq("USD"));
  }

  @Test
  void checkoutRejectsMissingPrincipalAndOtherPolicyholder() {
    stubPolicy();
    assertThrows(ResponseStatusException.class, () -> paymentService.startCheckout("POL-1", null));
    assertThrows(ResponseStatusException.class,
      () -> paymentService.startCheckout("POL-1", () -> "other@example.com"));
    verifyNoInteractions(checkoutProvider);
  }

  @Test
  void checkoutRejectsInactiveAndUnconfiguredOrZeroPremiums() {
    stubPolicy();
    policy.setStatus("inactive");
    assertThrows(ResponseStatusException.class, () -> paymentService.startCheckout("POL-1", owner));
    policy.setStatus("active");
    policy.setPremiumAmount(null);
    assertThrows(ResponseStatusException.class, () -> paymentService.startCheckout("POL-1", owner));
    policy.setPremiumAmount(BigDecimal.ZERO);
    assertThrows(ResponseStatusException.class, () -> paymentService.startCheckout("POL-1", owner));
    verifyNoInteractions(checkoutProvider);
  }

  @Test
  void checkoutRejectsNonUsdOrFractionalCentPremiums() {
    stubPolicy();
    policy.setCurrency("CAD");
    assertThrows(ResponseStatusException.class, () -> paymentService.startCheckout("POL-1", owner));
    policy.setCurrency("USD");
    policy.setPremiumAmount(new BigDecimal("1.001"));
    assertThrows(ResponseStatusException.class, () -> paymentService.startCheckout("POL-1", owner));
    verifyNoInteractions(checkoutProvider);
  }

  @ParameterizedTest
  @CsvSource({"PENDING", "SUCCEEDED"})
  void checkoutCannotCreateAnotherSessionForPendingOrPaidPolicy(PaymentStatus status) {
    stubPolicy();
    when(paymentRepository.findByPolicyIdOrderByCreatedAtDesc("POL-1"))
      .thenReturn(java.util.List.of(PolicyPayment.builder().status(status).build()));

    assertThrows(ResponseStatusException.class, () -> paymentService.startCheckout("POL-1", owner));
    verifyNoInteractions(checkoutProvider);
  }

  @Test
  void verifiedSuccessEventIsIdempotent() {
    PolicyPayment payment = PolicyPayment.builder()
      .id("payment-1")
      .checkoutSessionId("cs_test_123")
      .status(PaymentStatus.PENDING)
      .build();
    when(eventRepository.existsById("evt_1")).thenReturn(false, true);
    when(paymentRepository.findByCheckoutSessionId("cs_test_123")).thenReturn(Optional.of(payment));

    paymentService.processStripeEvent("evt_1", "checkout.session.completed", "cs_test_123", "pi_1", "paid");
    paymentService.processStripeEvent("evt_1", "checkout.session.completed", "cs_test_123", "pi_1", "paid");

    assertEquals(PaymentStatus.SUCCEEDED, payment.getStatus());
    assertEquals("pi_1", payment.getProviderPaymentIntentId());
    verify(paymentRepository, times(1)).save(payment);
    verify(eventRepository, times(1)).save(any());
  }

  @ParameterizedTest
  @CsvSource({
    "checkout.session.expired, CANCELLED",
    "checkout.session.async_payment_failed, FAILED"
  })
  void expiredAndFailedCheckoutEventsDoNotSucceed(String eventType, PaymentStatus expectedStatus) {
    PolicyPayment payment = PolicyPayment.builder()
      .checkoutSessionId("cs_test_123")
      .status(PaymentStatus.PENDING)
      .build();
    when(eventRepository.existsById(any())).thenReturn(false);
    when(paymentRepository.findByCheckoutSessionId("cs_test_123")).thenReturn(Optional.of(payment));

    paymentService.processStripeEvent("evt_" + eventType, eventType, "cs_test_123", null, "unpaid");

    assertEquals(expectedStatus, payment.getStatus());
  }
}
