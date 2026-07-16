package com.insurewell.controller;

import com.insurewell.model.Policy;
import com.insurewell.repository.PolicyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class PolicyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PolicyRepository policyRepository;

    @Test
    public void testGetRenewalReminderPdf_Success() throws Exception {
        // Create and save a policy
        Policy policy = Policy.builder()
                .id("POL-TEST-123")
                .holderName("John Doe")
                .planName("Test Premium Plan")
                .coverageAmount(200000.0)
                .status("active")
                .startDate("2024-01-01")
                .endDate("2026-12-31")
                .createdAt(LocalDateTime.now())
                .build();
        
        policyRepository.save(policy);

        // Call the renewal-reminder endpoint
        mockMvc.perform(get("/api/policies/POL-TEST-123/renewal-reminder/pdf"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"renewal-reminder-POL-TEST-123.pdf\""))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(result -> {
                    byte[] contentBytes = result.getResponse().getContentAsByteArray();
                    assert contentBytes.length > 0;
                });
    }

    @Test
    public void testGetRenewalReminderPdf_NotFound() throws Exception {
        // Call the renewal-reminder endpoint for unknown policy
        mockMvc.perform(get("/api/policies/UNKNOWN/renewal-reminder/pdf"))
                .andExpect(status().isNotFound());
    }
}
