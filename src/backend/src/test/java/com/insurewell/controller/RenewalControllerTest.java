package com.insurewell.controller;

import com.insurewell.model.Policy;
import com.insurewell.repository.PolicyRepository;
import com.insurewell.service.RenewalPdfService;
import com.insurewell.service.RenewalReminderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MockMvc tests for RenewalController endpoints.
 */
@WebMvcTest(RenewalController.class)
class RenewalControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private RenewalReminderService renewalReminderService;

  @MockBean
  private RenewalPdfService renewalPdfService;

  @MockBean
  private PolicyRepository policyRepository;

  @Test
  void getUpcomingRenewals_returns200WithList() throws Exception {
    String endDate = LocalDate.now().plusDays(10).format(DateTimeFormatter.ISO_DATE);
    when(renewalReminderService.getUpcomingRenewals())
        .thenReturn(List.of(new RenewalReminderService.RenewalReminderResult(
            "POL-001", "Jane Smith", endDate, 10L)));

    mockMvc.perform(get("/api/renewals/upcoming"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].policyId").value("POL-001"))
        .andExpect(jsonPath("$[0].holderName").value("Jane Smith"))
        .andExpect(jsonPath("$[0].daysUntilExpiry").value(10));
  }

  @Test
  void getUpcomingRenewals_returnsEmptyList() throws Exception {
    when(renewalReminderService.getUpcomingRenewals()).thenReturn(List.of());

    mockMvc.perform(get("/api/renewals/upcoming"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$").isEmpty());
  }

  @Test
  void remindPolicy_returns200ForExistingPolicy() throws Exception {
    when(renewalReminderService.remindPolicy("POL-001")).thenReturn(true);

    mockMvc.perform(post("/api/renewals/POL-001/remind"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));
  }

  @Test
  void remindPolicy_returns404ForNonExistentPolicy() throws Exception {
    when(renewalReminderService.remindPolicy("POL-999")).thenReturn(false);

    mockMvc.perform(post("/api/renewals/POL-999/remind"))
        .andExpect(status().isNotFound());
  }

  @Test
  void downloadRenewalPdf_returns200WithPdfContentType() throws Exception {
    Policy policy = new Policy();
    policy.setId("POL-001");
    policy.setHolderName("Jane Smith");
    policy.setPlanName("Gold Plan");
    policy.setCoverageAmount(75000.0);
    policy.setStatus("active");
    policy.setStartDate("2025-01-01");
    policy.setEndDate(LocalDate.now().plusDays(20).format(DateTimeFormatter.ISO_DATE));
    policy.setRenewalStatus("none");

    byte[] pdfBytes = new byte[]{0x25, 0x50, 0x44, 0x46}; // %PDF magic bytes
    when(policyRepository.findById("POL-001")).thenReturn(Optional.of(policy));
    when(renewalPdfService.generateRenewalPdf(policy)).thenReturn(pdfBytes);

    mockMvc.perform(get("/api/renewals/POL-001/pdf"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_PDF))
        .andExpect(header().string("Content-Disposition",
            "attachment; filename=\"renewal-notice-POL-001.pdf\""));
  }

  @Test
  void downloadRenewalPdf_returns404ForNonExistentPolicy() throws Exception {
    when(policyRepository.findById("POL-999")).thenReturn(Optional.empty());

    mockMvc.perform(get("/api/renewals/POL-999/pdf"))
        .andExpect(status().isNotFound());
  }
}
