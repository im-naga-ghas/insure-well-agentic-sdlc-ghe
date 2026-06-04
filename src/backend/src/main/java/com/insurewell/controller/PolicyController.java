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
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
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
  private static final String STATUS_ACTIVE = "active";
  private static final String STATUS_INACTIVE = "inactive";

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

  private boolean isValidStatus(String status) {
    return STATUS_ACTIVE.equals(status) || STATUS_INACTIVE.equals(status);
  }

  private boolean isValidDate(String date) {
    try {
      LocalDate.parse(date);
      return true;
    } catch (DateTimeParseException ex) {
      return false;
    }
  }

  private ResponseEntity<Map<String, String>> error(HttpStatus status, String message) {
    return ResponseEntity.status(status).body(Map.of("error", message));
  }

  @GetMapping
  public ResponseEntity<List<PolicyDTO>> getAllPolicies() {
    List<PolicyDTO> policies = policyRepository.findAllByOrderByCreatedAtAsc()
      .stream()
      .map(this::toDTO)
      .collect(Collectors.toList());
    return ResponseEntity.ok(policies);
  }

  @GetMapping("/{id}")
  public ResponseEntity<?> getPolicyById(@PathVariable String id) {
    Policy policy = policyRepository.findById(id).orElse(null);
    if (policy == null) {
      return error(HttpStatus.NOT_FOUND, "Policy not found");
    }
    return ResponseEntity.ok(toDTO(policy));
  }

  @PostMapping
  public ResponseEntity<?> createPolicy(@RequestBody PolicyDTO policyDTO) {
    if (policyDTO.getHolderName() == null || policyDTO.getHolderName().trim().isEmpty()
        || policyDTO.getPlanName() == null || policyDTO.getPlanName().trim().isEmpty()
        || policyDTO.getCoverageAmount() == null || policyDTO.getCoverageAmount() <= 0
        || policyDTO.getStartDate() == null || policyDTO.getStartDate().trim().isEmpty()
        || policyDTO.getEndDate() == null || policyDTO.getEndDate().trim().isEmpty()) {
      return error(HttpStatus.BAD_REQUEST, "holderName, planName, coverageAmount, startDate, and endDate are required");
    }

    if (!isValidDate(policyDTO.getStartDate()) || !isValidDate(policyDTO.getEndDate())) {
      return error(HttpStatus.BAD_REQUEST, "startDate and endDate must be valid ISO dates (YYYY-MM-DD)");
    }

    if (LocalDate.parse(policyDTO.getEndDate()).isBefore(LocalDate.parse(policyDTO.getStartDate()))) {
      return error(HttpStatus.BAD_REQUEST, "endDate must be on or after startDate");
    }

    if (policyDTO.getStatus() != null && !isValidStatus(policyDTO.getStatus())) {
      return error(HttpStatus.BAD_REQUEST, "status must be one of: active, inactive");
    }

    Policy policy = toEntity(policyDTO);
    Policy saved = policyRepository.save(policy);
    return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(saved));
  }

  @PatchMapping("/{id}")
  public ResponseEntity<?> updatePolicy(@PathVariable String id, @RequestBody PolicyDTO policyDTO) {
    Policy existing = policyRepository.findById(id).orElse(null);
    if (existing == null) {
      return error(HttpStatus.NOT_FOUND, "Policy not found");
    }

    if (policyDTO.getHolderName() != null && policyDTO.getHolderName().trim().isEmpty()) {
      return error(HttpStatus.BAD_REQUEST, "holderName cannot be blank");
    }
    if (policyDTO.getPlanName() != null && policyDTO.getPlanName().trim().isEmpty()) {
      return error(HttpStatus.BAD_REQUEST, "planName cannot be blank");
    }
    if (policyDTO.getCoverageAmount() != null && policyDTO.getCoverageAmount() <= 0) {
      return error(HttpStatus.BAD_REQUEST, "coverageAmount must be greater than 0");
    }
    if (policyDTO.getStatus() != null && !isValidStatus(policyDTO.getStatus())) {
      return error(HttpStatus.BAD_REQUEST, "status must be one of: active, inactive");
    }
    if (policyDTO.getStartDate() != null && !isValidDate(policyDTO.getStartDate())) {
      return error(HttpStatus.BAD_REQUEST, "startDate must be a valid ISO date (YYYY-MM-DD)");
    }
    if (policyDTO.getEndDate() != null && !isValidDate(policyDTO.getEndDate())) {
      return error(HttpStatus.BAD_REQUEST, "endDate must be a valid ISO date (YYYY-MM-DD)");
    }

    LocalDate startDate = policyDTO.getStartDate() != null
      ? LocalDate.parse(policyDTO.getStartDate())
      : LocalDate.parse(existing.getStartDate());
    LocalDate endDate = policyDTO.getEndDate() != null
      ? LocalDate.parse(policyDTO.getEndDate())
      : LocalDate.parse(existing.getEndDate());
    if (endDate.isBefore(startDate)) {
      return error(HttpStatus.BAD_REQUEST, "endDate must be on or after startDate");
    }

    if (policyDTO.getHolderName() != null) {
      existing.setHolderName(policyDTO.getHolderName().trim());
    }
    if (policyDTO.getPlanName() != null) {
      existing.setPlanName(policyDTO.getPlanName().trim());
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
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<?> deletePolicy(@PathVariable String id) {
    if (policyRepository.existsById(id)) {
      policyRepository.deleteById(id);
      return ResponseEntity.noContent().build();
    }
    return error(HttpStatus.NOT_FOUND, "Policy not found");
  }

}
