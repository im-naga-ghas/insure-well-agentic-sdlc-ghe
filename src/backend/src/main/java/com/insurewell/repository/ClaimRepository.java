package com.insurewell.repository;

import com.insurewell.model.Claim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Claim Repository
 * Spring Data JPA repository for Claim entity.
 */
@Repository
public interface ClaimRepository extends JpaRepository<Claim, String> {
  List<Claim> findByPolicyIdOrderBySubmittedAtDesc(String policyId);
  List<Claim> findAllByOrderBySubmittedAtDesc();
}
