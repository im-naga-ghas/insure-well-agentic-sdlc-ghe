package com.insurewell.controller;

import com.insurewell.model.Policy;
import com.insurewell.repository.PolicyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PolicyController.class)
class PolicyControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private PolicyRepository policyRepository;

  @Test
  void getExpiringSoonPoliciesReturnsPoliciesWithinThirtyDays() throws Exception {
    LocalDate today = LocalDate.now();
    LocalDateTime now = LocalDateTime.now();

    Policy expiringSoon = Policy.builder()
      .id("POL-SOON")
      .holderName("Soon Holder")
      .planName("Soon Plan")
      .coverageAmount(1000.0)
      .status("active")
      .startDate("2026-01-01")
      .endDate(today.plusDays(10).toString())
      .createdAt(now)
      .build();

    Policy expiringAtBoundary = Policy.builder()
      .id("POL-BOUNDARY")
      .holderName("Boundary Holder")
      .planName("Boundary Plan")
      .coverageAmount(1200.0)
      .status("active")
      .startDate("2026-01-01")
      .endDate(today.plusDays(30).toString())
      .createdAt(now.plusMinutes(1))
      .build();

    Policy notExpiringSoon = Policy.builder()
      .id("POL-LATER")
      .holderName("Later Holder")
      .planName("Later Plan")
      .coverageAmount(1500.0)
      .status("active")
      .startDate("2026-01-01")
      .endDate(today.plusDays(31).toString())
      .createdAt(now.plusMinutes(2))
      .build();

    Policy expired = Policy.builder()
      .id("POL-EXPIRED")
      .holderName("Expired Holder")
      .planName("Expired Plan")
      .coverageAmount(1800.0)
      .status("inactive")
      .startDate("2025-01-01")
      .endDate(today.minusDays(1).toString())
      .createdAt(now.plusMinutes(3))
      .build();

    Policy invalidDate = Policy.builder()
      .id("POL-INVALID")
      .holderName("Invalid Holder")
      .planName("Invalid Plan")
      .coverageAmount(2000.0)
      .status("active")
      .startDate("2026-01-01")
      .endDate("not-a-date")
      .createdAt(now.plusMinutes(4))
      .build();

    when(policyRepository.findAllByOrderByCreatedAtAsc()).thenReturn(
      List.of(expiringSoon, expiringAtBoundary, notExpiringSoon, expired, invalidDate)
    );

    mockMvc.perform(get("/api/policies/expiring-soon"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.length()").value(2))
      .andExpect(jsonPath("$[0].id").value("POL-SOON"))
      .andExpect(jsonPath("$[1].id").value("POL-BOUNDARY"));
  }
}
