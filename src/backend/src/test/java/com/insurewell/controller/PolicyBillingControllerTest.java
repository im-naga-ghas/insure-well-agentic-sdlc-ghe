package com.insurewell.controller;

import com.insurewell.dto.PolicyBillingRequest;
import com.insurewell.model.Policy;
import com.insurewell.repository.PolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class PolicyBillingControllerTest {
  private final PolicyRepository policyRepository = mock(PolicyRepository.class);
  private final PolicyController controller = new PolicyController();
  private final Policy policy = Policy.builder()
    .id("POL-1")
    .holderName("Policy Holder")
    .planName("Health Plan")
    .coverageAmount(1000.0)
    .status("active")
    .startDate("2026-01-01")
    .endDate("2026-12-31")
    .createdAt(LocalDateTime.now())
    .build();

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(controller, "policyRepository", policyRepository);
    ReflectionTestUtils.setField(controller, "staffPrincipals", "staff@example.com");
    when(policyRepository.findById("POL-1")).thenReturn(Optional.of(policy));
    when(policyRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void staffCanSetZeroUsdPremiumAndPolicyholder() {
    PolicyBillingRequest request = new PolicyBillingRequest();
    request.setPremiumAmount(BigDecimal.ZERO);
    request.setCurrency("USD");
    request.setOwnerPrincipal("holder@example.com");

    var response = controller.updateBilling("POL-1", request, (Principal) () -> "staff@example.com");

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(new BigDecimal("0.00"), policy.getPremiumAmount());
    assertEquals("holder@example.com", policy.getOwnerPrincipal());
  }

  @Test
  void rejectsUnauthorizedStaffAndInvalidPremiums() {
    PolicyBillingRequest request = new PolicyBillingRequest();
    request.setPremiumAmount(new BigDecimal("-0.01"));
    request.setCurrency("USD");

    assertEquals(HttpStatus.FORBIDDEN,
      controller.updateBilling("POL-1", request, (Principal) () -> "user@example.com").getStatusCode());
    assertEquals(HttpStatus.FORBIDDEN, controller.updateBilling("POL-1", request, null).getStatusCode());
    assertEquals(HttpStatus.BAD_REQUEST,
      controller.updateBilling("POL-1", request, (Principal) () -> "staff@example.com").getStatusCode());
    request.setPremiumAmount(BigDecimal.ONE);
    request.setCurrency("EUR");
    assertEquals(HttpStatus.BAD_REQUEST,
      controller.updateBilling("POL-1", request, (Principal) () -> "staff@example.com").getStatusCode());
    request.setCurrency("USD");
    request.setPremiumAmount(new BigDecimal("1.234"));
    assertEquals(HttpStatus.BAD_REQUEST,
      controller.updateBilling("POL-1", request, (Principal) () -> "staff@example.com").getStatusCode());
    assertEquals(HttpStatus.BAD_REQUEST,
      controller.updateBilling("POL-1", null, (Principal) () -> "staff@example.com").getStatusCode());
  }
}
