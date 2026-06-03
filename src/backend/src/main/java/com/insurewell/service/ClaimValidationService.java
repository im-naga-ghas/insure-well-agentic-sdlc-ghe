package com.insurewell.service;

import com.insurewell.model.Policy;
import com.insurewell.repository.ClaimRepository;
import com.insurewell.repository.PolicyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * ClaimValidationService
 * Centralised validation for claim submission inputs.
 */
@Service
public class ClaimValidationService {

  private static final long MAX_FILE_BYTES = 5 * 1024 * 1024; // 5 MB

  private static final byte[] MAGIC_PDF  = { 0x25, 0x50, 0x44, 0x46 };               // %PDF
  private static final byte[] MAGIC_JPEG = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF }; // JPEG SOI
  private static final byte[] MAGIC_PNG  = {
      (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A                          // PNG signature
  };

  @Autowired
  private PolicyRepository policyRepository;

  @Autowired
  private ClaimRepository claimRepository;

  /**
   * Validates all claim submission fields.
   *
   * @return a map of field → error message; empty if all inputs are valid.
   */
  public Map<String, String> validate(
      String policyId,
      Double amount,
      String description,
      MultipartFile file) {

    Map<String, String> errors = new LinkedHashMap<>();

    // --- policy_id ---
    Policy policy = validatePolicy(policyId, errors);

    // --- amount ---
    validateAmount(policyId, amount, policy, errors);

    // --- description ---
    validateDescription(description, errors);

    // --- file (optional) ---
    if (file != null && !file.isEmpty()) {
      validateFile(file, errors);
    }

    return errors;
  }

  private Policy validatePolicy(String policyId, Map<String, String> errors) {
    if (policyId == null || policyId.trim().isEmpty()) {
      errors.put("policy_id", "Policy ID is required");
      return null;
    }
    Optional<Policy> policyOpt = policyRepository.findById(policyId.trim());
    if (policyOpt.isEmpty()) {
      errors.put("policy_id", "Policy not found");
      return null;
    }
    Policy policy = policyOpt.get();
    if (!"active".equalsIgnoreCase(policy.getStatus())) {
      errors.put("policy_id", "Policy is not active");
      return null;
    }
    return policy;
  }

  private void validateAmount(String policyId, Double amount, Policy policy, Map<String, String> errors) {
    if (amount == null) {
      errors.put("amount", "Claim amount is required");
      return;
    }
    if (amount <= 0) {
      errors.put("amount", "Claim amount must be greater than 0");
      return;
    }
    if (policy != null && policyId != null && !policyId.trim().isEmpty()) {
      double used = claimRepository.sumActiveAmountByPolicyId(policyId.trim());
      double remaining = policy.getCoverageAmount() - used;
      if (amount > remaining) {
        errors.put("amount", String.format(
            "Claim amount exceeds remaining coverage of $%.2f", remaining));
      }
    }
  }

  private void validateDescription(String description, Map<String, String> errors) {
    if (description == null || description.trim().isEmpty()) {
      errors.put("description", "Description is required");
      return;
    }
    String trimmed = description.trim();
    if (trimmed.length() < 10) {
      errors.put("description", "Description must be at least 10 characters");
    } else if (trimmed.length() > 500) {
      errors.put("description", "Description must not exceed 500 characters");
    }
  }

  private void validateFile(MultipartFile file, Map<String, String> errors) {
    if (file.getSize() > MAX_FILE_BYTES) {
      errors.put("file", "File size must not exceed 5 MB");
      return;
    }
    try {
      byte[] header = file.getBytes();
      if (!isAllowedMimeType(header)) {
        errors.put("file", "Only PDF, JPG, JPEG, and PNG files are accepted");
      }
    } catch (IOException e) {
      errors.put("file", "Could not read uploaded file");
    }
  }

  private boolean isAllowedMimeType(byte[] bytes) {
    if (bytes.length >= MAGIC_PDF.length && startsWith(bytes, MAGIC_PDF)) {
      return true;
    }
    if (bytes.length >= MAGIC_JPEG.length && startsWith(bytes, MAGIC_JPEG)) {
      return true;
    }
    if (bytes.length >= MAGIC_PNG.length && startsWith(bytes, MAGIC_PNG)) {
      return true;
    }
    return false;
  }

  private boolean startsWith(byte[] data, byte[] prefix) {
    for (int i = 0; i < prefix.length; i++) {
      if (data[i] != prefix[i]) {
        return false;
      }
    }
    return true;
  }
}
