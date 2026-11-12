package com.insurewell.repository;

import com.insurewell.model.Policy;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Policy Repository
 * Spring Data JPA repository for Policy entity.
 */
@Repository
public interface PolicyRepository extends JpaRepository<Policy, String> {
  List<Policy> findAllByOrderByCreatedAtAsc();

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select policy from Policy policy where policy.id = :id")
  Optional<Policy> findByIdForPaymentLock(@Param("id") String id);
}
