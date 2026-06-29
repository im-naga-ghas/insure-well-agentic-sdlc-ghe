package com.insurewell.controller;

import com.insurewell.model.Policy;
import com.insurewell.repository.PolicyRepository;
import com.insurewell.service.RenewalPdfService;
import com.insurewell.service.RenewalReminderService;
import com.insurewell.service.RenewalReminderService.RenewalReminderResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Renewal REST Controller
 * Endpoints for listing upcoming renewals, triggering reminders, and downloading PDF notices.
 */
@RestController
@RequestMapping("/api/renewals")
@CrossOrigin(origins = "*")
public class RenewalController {

  @Autowired
  private RenewalReminderService renewalReminderService;

  @Autowired
  private RenewalPdfService renewalPdfService;

  @Autowired
  private PolicyRepository policyRepository;

  /**
   * GET /api/renewals/upcoming
   * Returns policies expiring within the configured window, sorted by endDate ascending.
   */
  @GetMapping("/upcoming")
  public ResponseEntity<List<RenewalReminderResult>> getUpcomingRenewals() {
    return ResponseEntity.ok(renewalReminderService.getUpcomingRenewals());
  }

  /**
   * POST /api/renewals/{policyId}/remind
   * Manually triggers a renewal reminder for a single policy.
   */
  @PostMapping("/{policyId}/remind")
  public ResponseEntity<Map<String, Object>> remindPolicy(@PathVariable String policyId) {
    boolean found = renewalReminderService.remindPolicy(policyId);
    if (!found) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(Map.of(
        "success", true,
        "message", "Renewal reminder sent for policy " + policyId
    ));
  }

  /**
   * GET /api/renewals/{policyId}/pdf
   * Streams a PDF renewal notice for the given policy.
   */
  @GetMapping("/{policyId}/pdf")
  public ResponseEntity<byte[]> downloadRenewalPdf(@PathVariable String policyId) {
    java.util.Optional<Policy> policyOpt = policyRepository.findById(policyId);
    if (policyOpt.isEmpty()) {
      return ResponseEntity.<byte[]>notFound().build();
    }
    try {
      byte[] pdfBytes = renewalPdfService.generateRenewalPdf(policyOpt.get());
      return ResponseEntity.ok()
          .contentType(MediaType.APPLICATION_PDF)
          .header(HttpHeaders.CONTENT_DISPOSITION,
              "attachment; filename=\"renewal-notice-" + policyId + ".pdf\"")
          .body(pdfBytes);
    } catch (IOException e) {
      return ResponseEntity.<byte[]>internalServerError().build();
    }
  }
}
