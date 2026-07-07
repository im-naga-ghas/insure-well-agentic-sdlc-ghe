package com.insurewell.controller;

import com.insurewell.model.Policy;
import com.insurewell.repository.ClaimRepository;
import com.insurewell.repository.PolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PolicyControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private PolicyRepository policyRepository;

  @Autowired
  private ClaimRepository claimRepository;

  @BeforeEach
  void setUp() {
    claimRepository.deleteAll();
    policyRepository.deleteAll();
  }

  @Test
  void expiringWithinWindowReturnsPolicy() throws Exception {
    policyRepository.save(policy("POL-EXP-15", "active", LocalDate.now().plusDays(15)));

    mockMvc.perform(get("/api/policies/expiring").param("days", "30"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.length()").value(1))
      .andExpect(jsonPath("$[0].id").value("POL-EXP-15"));
  }

  @Test
  void expiringOutsideWindowExcludesPolicy() throws Exception {
    policyRepository.save(policy("POL-EXP-60", "active", LocalDate.now().plusDays(60)));

    mockMvc.perform(get("/api/policies/expiring").param("days", "30"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void inactivePolicyExcluded() throws Exception {
    policyRepository.save(policy("POL-INACTIVE-05", "inactive", LocalDate.now().plusDays(5)));

    mockMvc.perform(get("/api/policies/expiring").param("days", "30"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void emptyListWhenNoPoliciesExpiring() throws Exception {
    mockMvc.perform(get("/api/policies/expiring").param("days", "30"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.length()").value(0));
  }

  private Policy policy(String id, String status, LocalDate endDate) {
    return Policy.builder()
      .id(id)
      .holderName("Test Holder")
      .planName("Test Plan")
      .coverageAmount(100000.0)
      .status(status)
      .startDate(LocalDate.now().minusYears(1).toString())
      .endDate(endDate.toString())
      .createdAt(LocalDateTime.now())
      .build();
  }
}
