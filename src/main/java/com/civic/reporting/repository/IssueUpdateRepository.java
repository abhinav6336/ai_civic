package com.civic.reporting.repository;

import com.civic.reporting.entity.IssueUpdate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IssueUpdateRepository extends JpaRepository<IssueUpdate, Long> {
    List<IssueUpdate> findByIssueIdOrderByCreatedAtDesc(Long issueId);
}
