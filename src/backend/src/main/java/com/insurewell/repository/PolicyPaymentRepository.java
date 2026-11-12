package com.insurewell.repository;

import com.insurewell.model.PolicyPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PolicyPaymentRepository extends JpaRepository<PolicyPayment, String> {
  List<PolicyPayment> findByPolicyIdOrderByCreatedAtDesc(String policyId);
  Optional<PolicyPayment> findByCheckoutSessionId(String checkoutSessionId);
}
