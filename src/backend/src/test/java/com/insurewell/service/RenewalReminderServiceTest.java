package com.insurewell.service;

import com.insurewell.model.Policy;
import com.insurewell.repository.PolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RenewalReminderService date-window logic.
 */
@ExtendWith(MockitoExtension.class)
class RenewalReminderServiceTest {

  @Mock
  private PolicyRepository policyRepository;

  @InjectMocks
  private RenewalReminderService service;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(service, "daysBefore", 30);
  }

  private Policy policy(String id, String endDate, String renewalStatus) {
    Policy p = new Policy();
    p.setId(id);
    p.setHolderName("Test Holder");
    p.setPlanName("Test Plan");
    p.setCoverageAmount(100000.0);
    p.setStatus("active");
    p.setStartDate("2025-01-01");
    p.setEndDate(endDate);
    p.setRenewalStatus(renewalStatus);
    return p;
  }

  @Test
  void getUpcomingRenewals_returnsResultsWithCorrectDays() {
    String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
    String cutoff = LocalDate.now().plusDays(30).format(DateTimeFormatter.ISO_DATE);
    String expiresIn15 = LocalDate.now().plusDays(15).format(DateTimeFormatter.ISO_DATE);

    Policy p = policy("POL-001", expiresIn15, "none");
    when(policyRepository.findByStatusAndEndDateBetweenOrderByEndDateAsc("active", today, cutoff))
        .thenReturn(List.of(p));

    List<RenewalReminderService.RenewalReminderResult> results = service.getUpcomingRenewals();

    assertThat(results).hasSize(1);
    assertThat(results.get(0).getPolicyId()).isEqualTo("POL-001");
    assertThat(results.get(0).getDaysUntilExpiry()).isEqualTo(15L);
  }

  @Test
  void checkAndSendRenewalReminders_skipsAlreadyRemindedPolicies() {
    String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
    String cutoff = LocalDate.now().plusDays(30).format(DateTimeFormatter.ISO_DATE);
    String expiresIn10 = LocalDate.now().plusDays(10).format(DateTimeFormatter.ISO_DATE);

    Policy alreadyReminded = policy("POL-002", expiresIn10, "reminded");
    when(policyRepository.findByStatusAndEndDateBetweenOrderByEndDateAsc("active", today, cutoff))
        .thenReturn(List.of(alreadyReminded));

    service.checkAndSendRenewalReminders();

    verify(policyRepository, never()).saveAll(anyList());
  }

  @Test
  void checkAndSendRenewalReminders_setsReminderOnNewPolicies() {
    String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
    String cutoff = LocalDate.now().plusDays(30).format(DateTimeFormatter.ISO_DATE);
    String expiresIn5 = LocalDate.now().plusDays(5).format(DateTimeFormatter.ISO_DATE);

    Policy fresh = policy("POL-003", expiresIn5, "none");
    when(policyRepository.findByStatusAndEndDateBetweenOrderByEndDateAsc("active", today, cutoff))
        .thenReturn(List.of(fresh));

    service.checkAndSendRenewalReminders();

    verify(policyRepository).saveAll(argThat(list -> {
      List<Policy> saved = (List<Policy>) list;
      return saved.size() == 1 && "reminded".equals(saved.get(0).getRenewalStatus());
    }));
  }

  @Test
  void checkAndSendRenewalReminders_policyAtDay30IsIncluded() {
    String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
    String cutoff = LocalDate.now().plusDays(30).format(DateTimeFormatter.ISO_DATE);
    String expiresIn30 = LocalDate.now().plusDays(30).format(DateTimeFormatter.ISO_DATE);

    Policy boundary = policy("POL-004", expiresIn30, "none");
    when(policyRepository.findByStatusAndEndDateBetweenOrderByEndDateAsc("active", today, cutoff))
        .thenReturn(List.of(boundary));

    List<RenewalReminderService.RenewalReminderResult> results = service.checkAndSendRenewalReminders();

    assertThat(results).hasSize(1);
    assertThat(results.get(0).getDaysUntilExpiry()).isEqualTo(30L);
  }

  @Test
  void getUpcomingRenewals_returnsEmptyWhenNoPoliciesExpireSoon() {
    String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
    String cutoff = LocalDate.now().plusDays(30).format(DateTimeFormatter.ISO_DATE);

    when(policyRepository.findByStatusAndEndDateBetweenOrderByEndDateAsc("active", today, cutoff))
        .thenReturn(List.of());

    List<RenewalReminderService.RenewalReminderResult> results = service.getUpcomingRenewals();

    assertThat(results).isEmpty();
  }

  @Test
  void remindPolicy_returnsFalseForNonExistentPolicy() {
    when(policyRepository.findById("POL-999")).thenReturn(Optional.empty());

    boolean result = service.remindPolicy("POL-999");

    assertThat(result).isFalse();
  }

  @Test
  void remindPolicy_setStatusAndTimestampForExistingPolicy() {
    Policy p = policy("POL-001", LocalDate.now().plusDays(5).format(DateTimeFormatter.ISO_DATE), "none");
    when(policyRepository.findById("POL-001")).thenReturn(Optional.of(p));

    boolean result = service.remindPolicy("POL-001");

    assertThat(result).isTrue();
    assertThat(p.getRenewalStatus()).isEqualTo("reminded");
    assertThat(p.getRenewalReminderSentAt()).isNotNull();
    verify(policyRepository).save(p);
  }
}
