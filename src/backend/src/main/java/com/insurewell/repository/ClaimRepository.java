package com.insurewell.repository;

import com.insurewell.model.Claim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

  @Query("SELECT COALESCE(SUM(c.amount), 0) FROM Claim c WHERE c.policyId = :policyId AND c.status IN ('Pending', 'Approved')")
  Double sumActiveAmountByPolicyId(@Param("policyId") String policyId);
}
