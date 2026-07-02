package com.insurewell.controller;

import com.insurewell.dto.ClaimDTO;
import com.insurewell.model.Claim;
import com.insurewell.repository.ClaimRepository;
import com.insurewell.repository.PolicyRepository;
import com.insurewell.security.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
public class ClaimController {

  @Autowired
  private ClaimRepository claimRepository;

  @Autowired
  private PolicyRepository policyRepository;

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
  public ResponseEntity<List<ClaimDTO>> getClaims(@RequestParam(required = false) String policy_id, Authentication authentication) {
    AuthenticatedUser user = currentUser(authentication);
    if (!user.isAdmin() && policy_id != null && !policy_id.isEmpty() && !policy_id.equals(user.getPolicyId())) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    String effectivePolicyId = user.isAdmin() ? policy_id : user.getPolicyId();
    List<ClaimDTO> claims;
    if (effectivePolicyId != null && !effectivePolicyId.isEmpty()) {
      claims = claimRepository.findByPolicyIdOrderBySubmittedAtDesc(effectivePolicyId)
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
      @RequestParam String description,
      Authentication authentication) {

    // Validate input
    if (policy_id == null || policy_id.trim().isEmpty()
        || amount == null || amount <= 0
        || description == null || description.trim().isEmpty()) {
      return ResponseEntity.badRequest()
        .body(Map.of("error", "policy_id, amount, and description are required"));
    }

    if (!canAccessPolicy(authentication, policy_id)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(Map.of("error", "You are not authorized to access this policy"));
    }

    // Check if policy exists
    if (!policyRepository.existsById(policy_id)) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(Map.of("error", "Policy not found"));
    }

    LocalDateTime now = LocalDateTime.now();
    String claimId = "CLM-" + System.currentTimeMillis();

    Claim claim = Claim.builder()
      .id(claimId)
      .policyId(policy_id)
      .amount(amount)
      .description(description)
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
      @RequestBody Map<String, String> body,
      Authentication authentication) {

    if (!isAdmin(authentication)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(Map.of("error", "Only admins can update claim status"));
    }

    String status = body.get("status");
    if (status == null || (!status.equals("Pending") && !status.equals("Approved") && !status.equals("Rejected"))) {
      return ResponseEntity.badRequest()
        .body(Map.of("error", "Status must be one of: Pending, Approved, Rejected"));
    }

    return claimRepository.findById(id)
      .map(claim -> {
        claim.setStatus(status);
        claim.setUpdatedAt(LocalDateTime.now());
        Claim updated = claimRepository.save(claim);
        return ResponseEntity.ok(toDTO(updated));
      })
      .orElse(ResponseEntity.notFound().build());
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteClaim(@PathVariable String id, Authentication authentication) {
    if (!isAdmin(authentication)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    if (claimRepository.existsById(id)) {
      claimRepository.deleteById(id);
      return ResponseEntity.noContent().build();
    }
    return ResponseEntity.notFound().build();
  }

  @GetMapping("/health")
  public ResponseEntity<Map<String, String>> health() {
    return ResponseEntity.ok(Map.of("status", "ok"));
  }

  private AuthenticatedUser currentUser(Authentication authentication) {
    return (AuthenticatedUser) authentication.getPrincipal();
  }

  private boolean isAdmin(Authentication authentication) {
    return currentUser(authentication).isAdmin();
  }

  private boolean canAccessPolicy(Authentication authentication, String policyId) {
    AuthenticatedUser user = currentUser(authentication);
    return user.isAdmin() || policyId.equals(user.getPolicyId());
  }

}
