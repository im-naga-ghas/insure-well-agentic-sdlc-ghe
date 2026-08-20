package com.insurewell.controller;

import com.insurewell.dto.ClaimDTO;
import com.insurewell.model.Claim;
import com.insurewell.repository.ClaimRepository;
import com.insurewell.repository.PolicyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Claim REST Controller
 * Endpoints: GET, POST, PATCH (status), DELETE for claims.
 */
@RestController
@RequestMapping("/api/claims")
@CrossOrigin(origins = "*")
public class ClaimController {

  @Autowired
  private ClaimRepository claimRepository;

  @Autowired
  private PolicyRepository policyRepository;

  private ResponseEntity<Map<String, String>> error(HttpStatus status, String message) {
    return ResponseEntity.status(status).body(Map.of("error", message));
  }

  private ClaimDTO toDTO(Claim claim) {
    return ClaimDTO.builder()
      .id(claim.getId())
      .policyId(claim.getPolicyId())
      .amount(claim.getAmount())
      .description(claim.getDescription())
      .status(claim.getStatus())
      .fileName(claim.getFileName())
      .submittedAt(claim.getSubmittedAt().format(DateTimeFormatter.ISO_DATE_TIME) + "Z")
      .updatedAt(claim.getUpdatedAt().format(DateTimeFormatter.ISO_DATE_TIME) + "Z")
      .build();
  }

  @GetMapping
  public ResponseEntity<List<ClaimDTO>> getClaims(@RequestParam(required = false) String policy_id) {
    List<ClaimDTO> claims;
    if (policy_id != null && !policy_id.isEmpty()) {
      claims = claimRepository.findByPolicyIdOrderBySubmittedAtDesc(policy_id)
        .stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
    } else {
      claims = claimRepository.findAllByOrderBySubmittedAtDesc()
        .stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
    }
    return ResponseEntity.ok(claims);
  }

  @PostMapping
  public ResponseEntity<?> createClaim(
      @RequestParam String policy_id,
      @RequestParam Double amount,
      @RequestParam String description) {
    String policyId = policy_id == null ? null : policy_id.trim();
    String claimDescription = description == null ? null : description.trim();

    // Validate input
    if (policyId == null || policyId.isEmpty()
        || amount == null || amount <= 0
        || claimDescription == null || claimDescription.isEmpty()) {
      return error(HttpStatus.BAD_REQUEST, "policy_id, amount, and description are required");
    }

    // Check if policy exists
    if (!policyRepository.existsById(policyId)) {
      return error(HttpStatus.NOT_FOUND, "Policy not found");
    }

    LocalDateTime now = LocalDateTime.now();
    String claimId = "CLM-" + System.currentTimeMillis();

    Claim claim = Claim.builder()
      .id(claimId)
      .policyId(policyId)
      .amount(amount)
      .description(claimDescription)
      .status("Pending")
      .fileName(null) // File upload would be handled separately
      .submittedAt(now)
      .updatedAt(now)
      .build();

    Claim saved = claimRepository.save(claim);
    return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(saved));
  }

  @PatchMapping("/{id}/status")
  public ResponseEntity<?> updateClaimStatus(
      @PathVariable String id,
      @RequestBody Map<String, String> body) {
    String status = body == null ? null : body.get("status");
    if (status == null || (!status.equals("Pending") && !status.equals("Approved") && !status.equals("Rejected"))) {
      return error(HttpStatus.BAD_REQUEST, "Status must be one of: Pending, Approved, Rejected");
    }

    Claim claim = claimRepository.findById(id).orElse(null);
    if (claim == null) {
      return error(HttpStatus.NOT_FOUND, "Claim not found");
    }

    claim.setStatus(status);
    claim.setUpdatedAt(LocalDateTime.now());
    Claim updated = claimRepository.save(claim);
    return ResponseEntity.ok(toDTO(updated));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<?> deleteClaim(@PathVariable String id) {
    if (claimRepository.existsById(id)) {
      claimRepository.deleteById(id);
      return ResponseEntity.noContent().build();
    }
    return error(HttpStatus.NOT_FOUND, "Claim not found");
  }

  @GetMapping("/health")
  public ResponseEntity<Map<String, String>> health() {
    return ResponseEntity.ok(Map.of("status", "ok"));
  }

}
