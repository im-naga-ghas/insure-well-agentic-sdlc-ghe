package com.insurewell.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AccessControlIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void policiesRequireAuthentication() throws Exception {
    mockMvc.perform(get("/api/policies"))
      .andExpect(status().isUnauthorized());
  }

  @Test
  void policyholderOnlySeesOwnPolicies() throws Exception {
    mockMvc.perform(get("/api/policies")
        .with(httpBasic("alex", "policy123")))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.length()").value(1))
      .andExpect(jsonPath("$[0].id").value("POL-2024-001"));
  }

  @Test
  void policyholderCannotAccessAnotherPolicy() throws Exception {
    mockMvc.perform(get("/api/policies/POL-2024-002")
        .with(httpBasic("alex", "policy123")))
      .andExpect(status().isForbidden());
  }

  @Test
  void policyholderCannotReadAnotherPolicyClaims() throws Exception {
    mockMvc.perform(get("/api/claims")
        .param("policy_id", "POL-2024-002")
        .with(httpBasic("alex", "policy123")))
      .andExpect(status().isForbidden());
  }

  @Test
  void policyholderCanCreateClaimForOwnPolicy() throws Exception {
    mockMvc.perform(post("/api/claims")
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .param("policy_id", "POL-2024-001")
        .param("amount", "250.0")
        .param("description", "Follow-up visit")
        .with(csrf())
        .with(httpBasic("alex", "policy123")))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.policyId").value("POL-2024-001"))
      .andExpect(jsonPath("$.status").value("Pending"));
  }

  @Test
  void policyholderCannotUpdateClaimStatus() throws Exception {
    mockMvc.perform(patch("/api/claims/CLM-1715787000000/status")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"status\":\"Rejected\"}")
        .with(csrf())
        .with(httpBasic("alex", "policy123")))
      .andExpect(status().isForbidden());
  }

  @Test
  void adminCanUpdateClaimStatus() throws Exception {
    mockMvc.perform(patch("/api/claims/CLM-1715787000000/status")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"status\":\"Rejected\"}")
        .with(csrf())
        .with(httpBasic("admin", "admin123")))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.id").value("CLM-1715787000000"))
      .andExpect(jsonPath("$.status").value("Rejected"));
  }
}
