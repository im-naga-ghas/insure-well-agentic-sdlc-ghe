package com.insurewell.controller;

import com.insurewell.dto.ClaimDTO;
import com.insurewell.model.Claim;
import com.insurewell.model.Policy;
import com.insurewell.repository.ClaimRepository;
import com.insurewell.repository.PolicyRepository;
import com.insurewell.security.UserProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

  @Autowired
  private UserProfiles userProfiles;

  private boolean isAdmin(Authentication authentication) {
    return authentication.getAuthorities().stream()
      .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
  }

  private boolean canAccessPolicy(Authentication authentication, Policy policy) {
    return isAdmin(authentication) || userProfiles.holderNameFor(authentication.getName())
      .map(holderName -> holderName.equals(policy.getHolderName()))
      .orElse(false);
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
  public ResponseEntity<List<ClaimDTO>> getClaims(@RequestParam(required = false) String policy_id, Authentication authentication) {
    List<ClaimDTO> claims;
    if (isAdmin(authentication)) {
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

    Set<String> userPolicyIds = userProfiles.holderNameFor(authentication.getName())
      .map(policyRepository::findByHolderNameOrderByCreatedAtAsc)
      .orElse(List.of())
      .stream()
      .map(Policy::getId)
      .collect(Collectors.toSet());

    if (policy_id != null && !policy_id.isEmpty() && !userPolicyIds.contains(policy_id)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    if (policy_id != null && !policy_id.isEmpty()) {
      claims = claimRepository.findByPolicyIdOrderBySubmittedAtDesc(policy_id)
        .stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
    } else {
      claims = userPolicyIds.isEmpty()
        ? List.of()
        : claimRepository.findByPolicyIdInOrderBySubmittedAtDesc(List.copyOf(userPolicyIds))
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

    Policy policy = policyRepository.findById(policy_id).orElse(null);
    if (policy == null) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(Map.of("error", "Policy not found"));
    }
    if (!canAccessPolicy(authentication, policy)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(Map.of("error", "Access denied"));
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
    Claim claim = claimRepository.findById(id).orElse(null);
    if (claim == null) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
    Policy policy = policyRepository.findById(claim.getPolicyId()).orElse(null);
    if (policy == null) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
    if (!canAccessPolicy(authentication, policy)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
    claimRepository.deleteById(id);
    return ResponseEntity.noContent().build();
  }

}
