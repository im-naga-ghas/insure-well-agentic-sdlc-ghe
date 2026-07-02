package com.insurewell.controller;

import com.insurewell.model.Claim;
import com.insurewell.repository.ClaimRepository;
import com.insurewell.repository.PolicyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClaimController.class)
class ClaimControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private ClaimRepository claimRepository;

  @MockBean
  private PolicyRepository policyRepository;

  @Test
  void createClaimRejectsNonFiniteAmount() throws Exception {
    mockMvc.perform(post("/api/claims")
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .param("policy_id", "POL-2024-001")
        .param("amount", "NaN")
        .param("description", "Annual exam"))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.error").value("policy_id, amount, and description are required"));

    verifyNoInteractions(policyRepository, claimRepository);
  }

  @Test
  void createClaimRejectsBlankDescription() throws Exception {
    mockMvc.perform(post("/api/claims")
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .param("policy_id", "POL-2024-001")
        .param("amount", "1500")
        .param("description", "   "))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.error").value("policy_id, amount, and description are required"));

    verifyNoInteractions(policyRepository, claimRepository);
  }

  @Test
  void createClaimTrimsInputsBeforeSaving() throws Exception {
    when(policyRepository.existsById("POL-2024-001")).thenReturn(true);
    when(claimRepository.save(any(Claim.class))).thenAnswer(invocation -> invocation.getArgument(0));

    mockMvc.perform(post("/api/claims")
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .param("policy_id", "  POL-2024-001  ")
        .param("amount", "1500")
        .param("description", "  Annual exam  "))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.policyId").value("POL-2024-001"))
      .andExpect(jsonPath("$.description").value("Annual exam"))
      .andExpect(jsonPath("$.amount").value(1500.0))
      .andExpect(jsonPath("$.status").value("Pending"));

    verify(policyRepository).existsById(eq("POL-2024-001"));
    verify(claimRepository).save(any(Claim.class));
  }
}
