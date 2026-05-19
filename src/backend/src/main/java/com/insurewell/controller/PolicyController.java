package com.insurewell.controller;

import com.insurewell.dto.PolicyDTO;
import com.insurewell.model.Policy;
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
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Policy REST Controller
 * Endpoints: GET, POST, PATCH, DELETE for policies.
 */
@RestController
@RequestMapping("/api/policies")
@CrossOrigin(origins = "*")
public class PolicyController {

  @Autowired
  private PolicyRepository policyRepository;

  private boolean isAdmin(Authentication authentication) {
    return authentication.getAuthorities().stream()
      .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
  }

  private boolean isOwner(Authentication authentication, Policy policy) {
    return UserProfiles.holderNameFor(authentication.getName())
      .map(holderName -> holderName.equals(policy.getHolderName()))
      .orElse(false);
  }

  private PolicyDTO toDTO(Policy policy) {
    return PolicyDTO.builder()
      .id(policy.getId())
      .holderName(policy.getHolderName())
      .planName(policy.getPlanName())
      .coverageAmount(policy.getCoverageAmount())
      .status(policy.getStatus())
      .startDate(policy.getStartDate())
      .endDate(policy.getEndDate())
      .createdAt(policy.getCreatedAt().format(DateTimeFormatter.ISO_DATE_TIME) + "Z")
      .build();
  }

  private Policy toEntity(PolicyDTO dto) {
    return Policy.builder()
      .id(dto.getId() != null ? dto.getId() : "POL-" + UUID.randomUUID().toString().substring(0, 8))
      .holderName(dto.getHolderName())
      .planName(dto.getPlanName())
      .coverageAmount(dto.getCoverageAmount())
      .status(dto.getStatus() != null ? dto.getStatus() : "active")
      .startDate(dto.getStartDate())
      .endDate(dto.getEndDate())
      .createdAt(LocalDateTime.now())
      .build();
  }

  @GetMapping
  public ResponseEntity<List<PolicyDTO>> getAllPolicies(Authentication authentication) {
    List<Policy> source = isAdmin(authentication)
      ? policyRepository.findAllByOrderByCreatedAtAsc()
      : UserProfiles.holderNameFor(authentication.getName())
      .map(policyRepository::findByHolderNameOrderByCreatedAtAsc)
      .orElse(List.of());

    List<PolicyDTO> policies = source
      .stream()
      .map(this::toDTO)
      .collect(Collectors.toList());
    return ResponseEntity.ok(policies);
  }

  @GetMapping("/{id}")
  public ResponseEntity<PolicyDTO> getPolicyById(@PathVariable String id, Authentication authentication) {
    return policyRepository.findById(id)
      .map(policy -> {
        if (isAdmin(authentication) || isOwner(authentication, policy)) {
          return ResponseEntity.ok(toDTO(policy));
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).<PolicyDTO>build();
      })
      .orElse(ResponseEntity.notFound().build());
  }

  @PostMapping
  public ResponseEntity<PolicyDTO> createPolicy(@RequestBody PolicyDTO policyDTO, Authentication authentication) {
    if (!isAdmin(authentication)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    if (policyDTO.getHolderName() == null || policyDTO.getHolderName().trim().isEmpty()
        || policyDTO.getPlanName() == null || policyDTO.getPlanName().trim().isEmpty()
        || policyDTO.getCoverageAmount() == null || policyDTO.getCoverageAmount() <= 0) {
      return ResponseEntity.badRequest().build();
    }

    Policy policy = toEntity(policyDTO);
    Policy saved = policyRepository.save(policy);
    return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(saved));
  }

  @PatchMapping("/{id}")
  public ResponseEntity<PolicyDTO> updatePolicy(@PathVariable String id, @RequestBody PolicyDTO policyDTO, Authentication authentication) {
    return policyRepository.findById(id)
      .map(existing -> {
        if (!isAdmin(authentication) && !isOwner(authentication, existing)) {
          return ResponseEntity.status(HttpStatus.FORBIDDEN).<PolicyDTO>build();
        }
        if (policyDTO.getHolderName() != null) {
          existing.setHolderName(policyDTO.getHolderName());
        }
        if (policyDTO.getPlanName() != null) {
          existing.setPlanName(policyDTO.getPlanName());
        }
        if (policyDTO.getCoverageAmount() != null) {
          existing.setCoverageAmount(policyDTO.getCoverageAmount());
        }
        if (policyDTO.getStatus() != null) {
          existing.setStatus(policyDTO.getStatus());
        }
        if (policyDTO.getStartDate() != null) {
          existing.setStartDate(policyDTO.getStartDate());
        }
        if (policyDTO.getEndDate() != null) {
          existing.setEndDate(policyDTO.getEndDate());
        }
        Policy updated = policyRepository.save(existing);
        return ResponseEntity.ok(toDTO(updated));
      })
      .orElse(ResponseEntity.notFound().build());
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deletePolicy(@PathVariable String id, Authentication authentication) {
    Policy policy = policyRepository.findById(id).orElse(null);
    if (policy == null) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
    if (!isAdmin(authentication) && !isOwner(authentication, policy)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
    policyRepository.deleteById(id);
    return ResponseEntity.noContent().build();
  }

}
