package com.insurewell.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
  "insurewell.security.mode=on",
  "insurewell.security.tenant-id=11111111-1111-1111-1111-111111111111",
  "insurewell.security.client-id=22222222-2222-2222-2222-222222222222"
})
class SecurityEnabledTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void healthEndpointIsPublic() throws Exception {
    mockMvc.perform(get("/api/health")).andExpect(status().isOk());
  }

  @Test
  void policiesRequireAccessToken() throws Exception {
    mockMvc.perform(get("/api/policies")).andExpect(status().isUnauthorized());
  }

  @Test
  void claimsRequireAccessToken() throws Exception {
    mockMvc.perform(get("/api/claims")).andExpect(status().isUnauthorized());
  }

  @Test
  void policiesAreAccessibleWithAccessToken() throws Exception {
    mockMvc.perform(get("/api/policies").with(jwt()))
      .andExpect(status().isOk());
  }

  @Test
  void meReturnsClaimsFromTheAccessToken() throws Exception {
    mockMvc.perform(get("/api/me").with(jwt().jwt(builder -> builder
        .claim("oid", "user-oid")
        .claim("name", "Ada Lovelace")
        .claim("preferred_username", "ada@insurewell.com")
        .claim("roles", java.util.List.of("Policy.Read"))
        .claim("scp", "Claims.ReadWrite"))))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.authenticated").value(true))
      .andExpect(jsonPath("$.id").value("user-oid"))
      .andExpect(jsonPath("$.name").value("Ada Lovelace"))
      .andExpect(jsonPath("$.username").value("ada@insurewell.com"))
      .andExpect(jsonPath("$.roles[0]").value("Policy.Read"))
      .andExpect(jsonPath("$.scopes[0]").value("Claims.ReadWrite"));
  }

  @Test
  void meRequiresAccessToken() throws Exception {
    mockMvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
  }
}
