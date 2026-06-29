package com.insurewell.repository;

import com.insurewell.model.Policy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Policy Repository
 * Spring Data JPA repository for Policy entity.
 */
@Repository
public interface PolicyRepository extends JpaRepository<Policy, String> {
  List<Policy> findAllByOrderByCreatedAtAsc();
  List<Policy> findByStatusAndEndDateBetweenOrderByEndDateAsc(String status, String fromDate, String toDate);
}
