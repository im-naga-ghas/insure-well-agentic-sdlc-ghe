package com.insurewell;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthRbacIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void unauthenticatedRequestsAreRejected() throws Exception {
    mockMvc.perform(get("/api/policies"))
      .andExpect(status().isUnauthorized());
  }

  @Test
  void policyholderCanOnlyViewOwnPolicies() throws Exception {
    MvcResult result = mockMvc.perform(get("/api/policies")
        .header("Authorization", basicAuth("alex", "alex123")))
      .andExpect(status().isOk())
      .andReturn();

    JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
    assertEquals(1, json.size());
    assertEquals("Alex Johnson", json.get(0).get("holderName").asText());
  }

  @Test
  void policyholderCannotViewOthersPolicyById() throws Exception {
    mockMvc.perform(get("/api/policies/POL-2024-002")
        .header("Authorization", basicAuth("alex", "alex123")))
      .andExpect(status().isForbidden());
  }

  @Test
  void policyholderCannotUpdateClaimStatus() throws Exception {
    mockMvc.perform(patch("/api/claims/CLM-1717264800000/status")
        .header("Authorization", basicAuth("alex", "alex123"))
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"status\":\"Approved\"}"))
      .andExpect(status().isForbidden());
  }

  @Test
  void policyholderCanCreateClaimForOwnedPolicyOnly() throws Exception {
    mockMvc.perform(post("/api/claims")
        .header("Authorization", basicAuth("alex", "alex123"))
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .param("policy_id", "POL-2024-001")
        .param("amount", "100")
        .param("description", "Own policy claim"))
      .andExpect(status().isCreated());

    mockMvc.perform(post("/api/claims")
        .header("Authorization", basicAuth("alex", "alex123"))
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .param("policy_id", "POL-2024-002")
        .param("amount", "100")
        .param("description", "Other policy claim"))
      .andExpect(status().isForbidden());
  }

  @Test
  void adminCanViewAllPoliciesAndClaims() throws Exception {
    MvcResult policies = mockMvc.perform(get("/api/policies")
        .header("Authorization", basicAuth("admin", "admin123")))
      .andExpect(status().isOk())
      .andReturn();

    JsonNode policiesJson = objectMapper.readTree(policies.getResponse().getContentAsString());
    assertTrue(policiesJson.size() >= 3);

    MvcResult claims = mockMvc.perform(get("/api/claims")
        .header("Authorization", basicAuth("admin", "admin123")))
      .andExpect(status().isOk())
      .andReturn();

    JsonNode claimsJson = objectMapper.readTree(claims.getResponse().getContentAsString());
    assertTrue(claimsJson.size() >= 7);
  }

  private String basicAuth(String username, String password) {
    String token = Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
    return "Basic " + token;
  }
}
