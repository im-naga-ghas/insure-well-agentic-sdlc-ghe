package com.insurewell.service;

import com.insurewell.model.Policy;
import com.insurewell.repository.ClaimRepository;
import com.insurewell.repository.PolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ClaimValidationService.
 */
@ExtendWith(MockitoExtension.class)
class ClaimValidationServiceTest {

  @Mock
  private PolicyRepository policyRepository;

  @Mock
  private ClaimRepository claimRepository;

  @InjectMocks
  private ClaimValidationService service;

  private Policy activePolicy;

  @BeforeEach
  void setUp() {
    activePolicy = Policy.builder()
        .id("POL-001")
        .holderName("Alice")
        .planName("Basic")
        .coverageAmount(10_000.0)
        .status("active")
        .startDate("2024-01-01")
        .endDate("2025-01-01")
        .build();
  }

  // ── policy_id ────────────────────────────────────────────────────────────

  @Test
  void missingPolicyId_returnsError() {
    Map<String, String> errors = service.validate(null, 100.0, "Valid description here", null);
    assertThat(errors).containsKey("policy_id");
  }

  @Test
  void blankPolicyId_returnsError() {
    Map<String, String> errors = service.validate("  ", 100.0, "Valid description here", null);
    assertThat(errors).containsKey("policy_id");
  }

  @Test
  void nonExistentPolicy_returnsError() {
    when(policyRepository.findById("UNKNOWN")).thenReturn(Optional.empty());
    Map<String, String> errors = service.validate("UNKNOWN", 100.0, "Valid description here", null);
    assertThat(errors).containsKey("policy_id");
    assertThat(errors.get("policy_id")).contains("not found");
  }

  @Test
  void inactivePolicy_returnsError() {
    Policy inactive = Policy.builder()
        .id("POL-999")
        .holderName("Bob")
        .planName("Basic")
        .coverageAmount(5_000.0)
        .status("inactive")
        .startDate("2024-01-01")
        .endDate("2025-01-01")
        .build();
    when(policyRepository.findById("POL-999")).thenReturn(Optional.of(inactive));
    Map<String, String> errors = service.validate("POL-999", 100.0, "Valid description here", null);
    assertThat(errors).containsKey("policy_id");
    assertThat(errors.get("policy_id")).contains("not active");
  }

  // ── amount ───────────────────────────────────────────────────────────────

  @Test
  void missingAmount_returnsError() {
    when(policyRepository.findById("POL-001")).thenReturn(Optional.of(activePolicy));
    Map<String, String> errors = service.validate("POL-001", null, "Valid description here", null);
    assertThat(errors).containsKey("amount");
  }

  @Test
  void zeroAmount_returnsError() {
    when(policyRepository.findById("POL-001")).thenReturn(Optional.of(activePolicy));
    Map<String, String> errors = service.validate("POL-001", 0.0, "Valid description here", null);
    assertThat(errors).containsKey("amount");
  }

  @Test
  void negativeAmount_returnsError() {
    when(policyRepository.findById("POL-001")).thenReturn(Optional.of(activePolicy));
    Map<String, String> errors = service.validate("POL-001", -50.0, "Valid description here", null);
    assertThat(errors).containsKey("amount");
  }

  @Test
  void amountExceedsCoverage_returnsError() {
    when(policyRepository.findById("POL-001")).thenReturn(Optional.of(activePolicy));
    when(claimRepository.sumActiveAmountByPolicyId("POL-001")).thenReturn(9_500.0);
    Map<String, String> errors = service.validate("POL-001", 600.0, "Valid description here", null);
    assertThat(errors).containsKey("amount");
    assertThat(errors.get("amount")).contains("remaining coverage");
  }

  @Test
  void amountAtBoundaryExactlyRemainingCoverage_isValid() {
    when(policyRepository.findById("POL-001")).thenReturn(Optional.of(activePolicy));
    when(claimRepository.sumActiveAmountByPolicyId("POL-001")).thenReturn(0.0);
    Map<String, String> errors = service.validate("POL-001", 10_000.0, "Valid description here", null);
    assertThat(errors).doesNotContainKey("amount");
  }

  // ── description ──────────────────────────────────────────────────────────

  @Test
  void missingDescription_returnsError() {
    when(policyRepository.findById("POL-001")).thenReturn(Optional.of(activePolicy));
    when(claimRepository.sumActiveAmountByPolicyId("POL-001")).thenReturn(0.0);
    Map<String, String> errors = service.validate("POL-001", 100.0, null, null);
    assertThat(errors).containsKey("description");
  }

  @Test
  void blankDescription_returnsError() {
    when(policyRepository.findById("POL-001")).thenReturn(Optional.of(activePolicy));
    when(claimRepository.sumActiveAmountByPolicyId("POL-001")).thenReturn(0.0);
    Map<String, String> errors = service.validate("POL-001", 100.0, "   ", null);
    assertThat(errors).containsKey("description");
  }

  @Test
  void tooShortDescription_returnsError() {
    when(policyRepository.findById("POL-001")).thenReturn(Optional.of(activePolicy));
    when(claimRepository.sumActiveAmountByPolicyId("POL-001")).thenReturn(0.0);
    Map<String, String> errors = service.validate("POL-001", 100.0, "short", null);
    assertThat(errors).containsKey("description");
    assertThat(errors.get("description")).contains("at least 10");
  }

  @Test
  void tooLongDescription_returnsError() {
    when(policyRepository.findById("POL-001")).thenReturn(Optional.of(activePolicy));
    when(claimRepository.sumActiveAmountByPolicyId("POL-001")).thenReturn(0.0);
    String longDesc = "a".repeat(501);
    Map<String, String> errors = service.validate("POL-001", 100.0, longDesc, null);
    assertThat(errors).containsKey("description");
    assertThat(errors.get("description")).contains("500");
  }

  // ── file ─────────────────────────────────────────────────────────────────

  @Test
  void oversizedFile_returnsError() {
    when(policyRepository.findById("POL-001")).thenReturn(Optional.of(activePolicy));
    when(claimRepository.sumActiveAmountByPolicyId("POL-001")).thenReturn(0.0);
    byte[] bigContent = new byte[6 * 1024 * 1024]; // 6 MB
    // Fill with PDF magic bytes so type check passes (size checked first)
    bigContent[0] = 0x25; bigContent[1] = 0x50; bigContent[2] = 0x44; bigContent[3] = 0x46;
    MockMultipartFile bigFile = new MockMultipartFile("file", "big.pdf", "application/pdf", bigContent);
    Map<String, String> errors = service.validate("POL-001", 100.0, "Valid description here", bigFile);
    assertThat(errors).containsKey("file");
    assertThat(errors.get("file")).contains("5 MB");
  }

  @Test
  void disallowedFileType_returnsError() {
    when(policyRepository.findById("POL-001")).thenReturn(Optional.of(activePolicy));
    when(claimRepository.sumActiveAmountByPolicyId("POL-001")).thenReturn(0.0);
    // .exe magic bytes (MZ header)
    byte[] exeContent = new byte[]{ 0x4D, 0x5A, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
    MockMultipartFile badFile = new MockMultipartFile("file", "malware.exe", "application/octet-stream", exeContent);
    Map<String, String> errors = service.validate("POL-001", 100.0, "Valid description here", badFile);
    assertThat(errors).containsKey("file");
    assertThat(errors.get("file")).contains("PDF, JPG");
  }

  @Test
  void validPdfFile_isAccepted() {
    when(policyRepository.findById("POL-001")).thenReturn(Optional.of(activePolicy));
    when(claimRepository.sumActiveAmountByPolicyId("POL-001")).thenReturn(0.0);
    byte[] pdfContent = new byte[]{ 0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34 }; // %PDF-1.4
    MockMultipartFile pdfFile = new MockMultipartFile("file", "doc.pdf", "application/pdf", pdfContent);
    Map<String, String> errors = service.validate("POL-001", 100.0, "Valid description here", pdfFile);
    assertThat(errors).doesNotContainKey("file");
  }

  @Test
  void validJpegFile_isAccepted() {
    when(policyRepository.findById("POL-001")).thenReturn(Optional.of(activePolicy));
    when(claimRepository.sumActiveAmountByPolicyId("POL-001")).thenReturn(0.0);
    byte[] jpegContent = new byte[]{ (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10 };
    MockMultipartFile jpegFile = new MockMultipartFile("file", "photo.jpg", "image/jpeg", jpegContent);
    Map<String, String> errors = service.validate("POL-001", 100.0, "Valid description here", jpegFile);
    assertThat(errors).doesNotContainKey("file");
  }

  @Test
  void validPngFile_isAccepted() {
    when(policyRepository.findById("POL-001")).thenReturn(Optional.of(activePolicy));
    when(claimRepository.sumActiveAmountByPolicyId("POL-001")).thenReturn(0.0);
    byte[] pngContent = new byte[]{
        (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00
    };
    MockMultipartFile pngFile = new MockMultipartFile("file", "image.png", "image/png", pngContent);
    Map<String, String> errors = service.validate("POL-001", 100.0, "Valid description here", pngFile);
    assertThat(errors).doesNotContainKey("file");
  }

  // ── happy path ───────────────────────────────────────────────────────────

  @Test
  void allValidInputsNoFile_returnsNoErrors() {
    when(policyRepository.findById("POL-001")).thenReturn(Optional.of(activePolicy));
    when(claimRepository.sumActiveAmountByPolicyId("POL-001")).thenReturn(0.0);
    Map<String, String> errors = service.validate("POL-001", 500.0, "Annual physical exam and follow-up", null);
    assertThat(errors).isEmpty();
  }
}
