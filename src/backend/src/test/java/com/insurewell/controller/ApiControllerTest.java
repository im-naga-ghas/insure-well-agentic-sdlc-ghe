package com.insurewell.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ApiController.class)
class ApiControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void apiRootReturnsServiceMetadata() throws Exception {
    mockMvc.perform(get("/api"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.name").value("InsureWell API"))
      .andExpect(jsonPath("$.status").value("ok"))
      .andExpect(jsonPath("$.endpoints.health").value("/api/health"))
      .andExpect(jsonPath("$.endpoints.policies").value("/api/policies"))
      .andExpect(jsonPath("$.endpoints.claims").value("/api/claims"));
  }

  @Test
  void healthReturnsStatusAndTimestamp() throws Exception {
    mockMvc.perform(get("/api/health"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("ok"))
      .andExpect(jsonPath("$.timestamp").isNotEmpty());
  }
}
