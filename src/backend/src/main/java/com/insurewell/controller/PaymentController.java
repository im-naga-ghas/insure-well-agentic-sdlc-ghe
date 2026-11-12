package com.insurewell.controller;

import com.insurewell.dto.CheckoutResponse;
import com.insurewell.dto.PaymentStatusDTO;
import com.insurewell.service.CheckoutProviderException;
import com.insurewell.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;

@RestController
@RequestMapping("/api/policies/{policyId}/payments")
@CrossOrigin(origins = "*")
public class PaymentController {
  private final PaymentService paymentService;

  public PaymentController(PaymentService paymentService) {
    this.paymentService = paymentService;
  }

  @PostMapping("/checkout")
  public ResponseEntity<CheckoutResponse> createCheckout(@PathVariable String policyId, Principal principal) {
    try {
      return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.startCheckout(policyId, principal));
    } catch (CheckoutProviderException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Checkout is temporarily unavailable");
    }
  }

  @GetMapping("/latest")
  public ResponseEntity<PaymentStatusDTO> latestPayment(@PathVariable String policyId, Principal principal) {
    return ResponseEntity.ok(paymentService.latestPayment(policyId, principal));
  }
}
