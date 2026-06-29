package com.insurewell.controller;

import com.insurewell.dto.PolicyDTO;
import com.insurewell.model.Policy;
import com.insurewell.repository.PolicyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
  public ResponseEntity<List<PolicyDTO>> getAllPolicies() {
    List<PolicyDTO> policies = policyRepository.findAllByOrderByCreatedAtAsc()
      .stream()
      .map(this::toDTO)
      .collect(Collectors.toList());
    return ResponseEntity.ok(policies);
  }

  @GetMapping("/expiring-soon")
  public ResponseEntity<List<PolicyDTO>> getExpiringSoonPolicies() {
    LocalDate today = LocalDate.now();
    LocalDate cutoffDate = today.plusDays(30);

    List<PolicyDTO> policies = policyRepository.findAllByOrderByCreatedAtAsc()
      .stream()
      .filter(policy -> {
        try {
          LocalDate endDate = LocalDate.parse(policy.getEndDate());
          return !endDate.isBefore(today) && !endDate.isAfter(cutoffDate);
        } catch (DateTimeParseException ex) {
          return false;
        }
      })
      .map(this::toDTO)
      .collect(Collectors.toList());

    return ResponseEntity.ok(policies);
  }

  @GetMapping("/{id}")
  public ResponseEntity<PolicyDTO> getPolicyById(@PathVariable String id) {
    return policyRepository.findById(id)
      .map(policy -> ResponseEntity.ok(toDTO(policy)))
      .orElse(ResponseEntity.notFound().build());
  }

  @PostMapping
  public ResponseEntity<PolicyDTO> createPolicy(@RequestBody PolicyDTO policyDTO) {
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
  public ResponseEntity<PolicyDTO> updatePolicy(@PathVariable String id, @RequestBody PolicyDTO policyDTO) {
    return policyRepository.findById(id)
      .map(existing -> {
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
  public ResponseEntity<Void> deletePolicy(@PathVariable String id) {
    if (policyRepository.existsById(id)) {
      policyRepository.deleteById(id);
      return ResponseEntity.noContent().build();
    }
    return ResponseEntity.notFound().build();
  }

}
