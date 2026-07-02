package com.insurewell.service;

import com.insurewell.model.Policy;
import com.insurewell.repository.PolicyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Renewal Reminder Service
 * Scheduled job that detects expiring policies and marks them for renewal.
 */
@Service
public class RenewalReminderService {

  @Autowired
  private PolicyRepository policyRepository;

  @Value("${renewal.reminder.days-before:30}")
  private int daysBefore;

  /**
   * Scheduled daily at 08:00 – checks for active policies expiring within the configured window.
   */
  @Scheduled(cron = "0 0 8 * * *")
  public List<RenewalReminderResult> checkAndSendRenewalReminders() {
    String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
    String cutoff = LocalDate.now().plusDays(daysBefore).format(DateTimeFormatter.ISO_DATE);

    List<Policy> expiring = policyRepository.findByStatusAndEndDateBetweenOrderByEndDateAsc(
        "active", today, cutoff);

    List<Policy> toRemind = expiring.stream()
        .filter(p -> !"reminded".equals(p.getRenewalStatus()))
        .collect(Collectors.toList());

    LocalDateTime now = LocalDateTime.now();
    toRemind.forEach(p -> {
      p.setRenewalReminderSentAt(now);
      p.setRenewalStatus("reminded");
    });
    if (!toRemind.isEmpty()) {
      policyRepository.saveAll(toRemind);
    }

    return expiring.stream()
        .map(p -> {
          LocalDate endDate = LocalDate.parse(p.getEndDate(), DateTimeFormatter.ISO_DATE);
          long days = ChronoUnit.DAYS.between(LocalDate.now(), endDate);
          return new RenewalReminderResult(p.getId(), p.getHolderName(), p.getEndDate(), days);
        })
        .collect(Collectors.toList());
  }

  /**
   * Get upcoming renewals without triggering reminders (used by controller).
   */
  public List<RenewalReminderResult> getUpcomingRenewals() {
    String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
    String cutoff = LocalDate.now().plusDays(daysBefore).format(DateTimeFormatter.ISO_DATE);

    return policyRepository.findByStatusAndEndDateBetweenOrderByEndDateAsc("active", today, cutoff)
        .stream()
        .map(p -> {
          LocalDate endDate = LocalDate.parse(p.getEndDate(), DateTimeFormatter.ISO_DATE);
          long days = ChronoUnit.DAYS.between(LocalDate.now(), endDate);
          return new RenewalReminderResult(p.getId(), p.getHolderName(), p.getEndDate(), days);
        })
        .collect(Collectors.toList());
  }

  /**
   * Manually trigger a reminder for a single policy.
   */
  public boolean remindPolicy(String policyId) {
    return policyRepository.findById(policyId).map(p -> {
      p.setRenewalReminderSentAt(LocalDateTime.now());
      p.setRenewalStatus("reminded");
      policyRepository.save(p);
      return true;
    }).orElse(false);
  }

  public int getDaysBefore() {
    return daysBefore;
  }

  /**
   * Immutable result record for a renewal reminder entry.
   */
  public static class RenewalReminderResult {
    private final String policyId;
    private final String holderName;
    private final String endDate;
    private final long daysUntilExpiry;

    public RenewalReminderResult(String policyId, String holderName, String endDate, long daysUntilExpiry) {
      this.policyId = policyId;
      this.holderName = holderName;
      this.endDate = endDate;
      this.daysUntilExpiry = daysUntilExpiry;
    }

    public String getPolicyId() { return policyId; }
    public String getHolderName() { return holderName; }
    public String getEndDate() { return endDate; }
    public long getDaysUntilExpiry() { return daysUntilExpiry; }
  }
}
