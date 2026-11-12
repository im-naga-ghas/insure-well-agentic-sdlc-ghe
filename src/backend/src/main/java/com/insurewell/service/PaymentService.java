package com.insurewell.service;

import com.insurewell.dto.CheckoutResponse;
import com.insurewell.dto.PaymentStatusDTO;
import com.insurewell.model.PaymentStatus;
import com.insurewell.model.Policy;
import com.insurewell.model.PolicyPayment;
import com.insurewell.model.ProcessedStripeEvent;
import com.insurewell.repository.PolicyPaymentRepository;
import com.insurewell.repository.PolicyRepository;
import com.insurewell.repository.ProcessedStripeEventRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.Locale;
import java.util.UUID;

@Service
public class PaymentService {
  private final PolicyRepository policyRepository;
  private final PolicyPaymentRepository paymentRepository;
  private final ProcessedStripeEventRepository eventRepository;
  private final CheckoutProvider checkoutProvider;

  public PaymentService(
      PolicyRepository policyRepository,
      PolicyPaymentRepository paymentRepository,
      ProcessedStripeEventRepository eventRepository,
      CheckoutProvider checkoutProvider) {
    this.policyRepository = policyRepository;
    this.paymentRepository = paymentRepository;
    this.eventRepository = eventRepository;
    this.checkoutProvider = checkoutProvider;
  }

  @Transactional(noRollbackFor = CheckoutProviderException.class)
  public CheckoutResponse startCheckout(String policyId, Principal principal) {
    String principalName = requirePrincipal(principal);
    Policy policy = policyRepository.findByIdForPaymentLock(policyId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    requireOwner(policy, principalName);
    if (!"active".equalsIgnoreCase(policy.getStatus())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Only active policies can be paid");
    }

    BigDecimal amount = policy.getPremiumAmount();
    if (amount == null || amount.signum() <= 0 || amount.scale() > 2
        || !"USD".equalsIgnoreCase(policy.getCurrency())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Policy does not have a payable USD premium");
    }
    boolean paymentAlreadyActive = paymentRepository.findByPolicyIdOrderByCreatedAtDesc(policyId).stream()
      .findFirst()
      .map(existing -> existing.getStatus() == PaymentStatus.PENDING
        || existing.getStatus() == PaymentStatus.SUCCEEDED)
      .orElse(false);
    if (paymentAlreadyActive) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "A payment is already pending or confirmed");
    }

    String paymentId = UUID.randomUUID().toString();
    PolicyPayment payment = PolicyPayment.builder()
      .id(paymentId)
      .policy(policy)
      .amount(amount)
      .currency("USD")
      .status(PaymentStatus.PENDING)
      .build();
    paymentRepository.saveAndFlush(payment);

    try {
      CheckoutSession session = checkoutProvider.createSession(paymentId, policyId, amount, "USD");
      payment.setCheckoutSessionId(session.id());
      paymentRepository.save(payment);
      return new CheckoutResponse(paymentId, session.url());
    } catch (CheckoutProviderException ex) {
      payment.setStatus(PaymentStatus.FAILED);
      paymentRepository.save(payment);
      throw ex;
    }
  }

  @Transactional(readOnly = true)
  public PaymentStatusDTO latestPayment(String policyId, Principal principal) {
    String principalName = requirePrincipal(principal);
    Policy policy = policyRepository.findById(policyId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    requireOwner(policy, principalName);
    return paymentRepository.findByPolicyIdOrderByCreatedAtDesc(policyId).stream()
      .findFirst()
      .map(PaymentService::toDTO)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  @Transactional
  public void processStripeEvent(String eventId, String eventType, String sessionId, String paymentIntentId,
      String providerPaymentStatus) {
    if (eventRepository.existsById(eventId)) {
      return;
    }

    paymentRepository.findByCheckoutSessionId(sessionId).ifPresent(payment -> {
      if (payment.getStatus() != PaymentStatus.PENDING) {
        return;
      }

      switch (eventType) {
        case "checkout.session.completed" -> {
          if ("paid".equalsIgnoreCase(providerPaymentStatus)) {
            payment.setStatus(PaymentStatus.SUCCEEDED);
          }
        }
        case "checkout.session.async_payment_succeeded" -> payment.setStatus(PaymentStatus.SUCCEEDED);
        case "checkout.session.async_payment_failed" -> payment.setStatus(PaymentStatus.FAILED);
        case "checkout.session.expired" -> payment.setStatus(PaymentStatus.CANCELLED);
        default -> {
          return;
        }
      }

      if (paymentIntentId != null) {
        payment.setProviderPaymentIntentId(paymentIntentId);
      }
      paymentRepository.save(payment);
    });

    eventRepository.save(ProcessedStripeEvent.builder().id(eventId).build());
  }

  private static String requirePrincipal(Principal principal) {
    if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }
    return principal.getName();
  }

  private static void requireOwner(Policy policy, String principalName) {
    String owner = policy.getOwnerPrincipal();
    if (owner == null || !owner.trim().toLowerCase(Locale.ROOT)
        .equals(principalName.trim().toLowerCase(Locale.ROOT))) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
  }

  private static PaymentStatusDTO toDTO(PolicyPayment payment) {
    return new PaymentStatusDTO(payment.getId(), payment.getAmount(), payment.getCurrency(),
      payment.getStatus().name(), payment.getCreatedAt(), payment.getUpdatedAt());
  }
}
