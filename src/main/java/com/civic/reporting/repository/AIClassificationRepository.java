package com.civic.reporting.repository;

import com.civic.reporting.entity.AIClassification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AIClassificationRepository extends JpaRepository<AIClassification, Long> {
    List<AIClassification> findByIssueIdOrderByProcessedAtDesc(Long issueId);
    Optional<AIClassification> findFirstByIssueIdOrderByProcessedAtDesc(Long issueId);
}
