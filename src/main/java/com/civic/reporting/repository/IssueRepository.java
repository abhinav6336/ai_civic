package com.civic.reporting.repository;

import com.civic.reporting.entity.Issue;
import com.civic.reporting.enums.IssueCategory;
import com.civic.reporting.enums.IssueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IssueRepository extends JpaRepository<Issue, Long> {

    Optional<Issue> findByTrackingNumber(String trackingNumber);

    List<Issue> findByCitizenIdOrderByCreatedAtDesc(Long citizenId);

    List<Issue> findByStatus(IssueStatus status);

    List<Issue> findByCategory(IssueCategory category);

    List<Issue> findByAssignedDepartmentId(Long departmentId);

    @Query("SELECT i FROM Issue i " +
           "LEFT JOIN i.assignedDepartment d " +
           "WHERE (:status IS NULL OR i.status = :status) AND " +
           "(:category IS NULL OR i.category = :category) AND " +
           "(:departmentId IS NULL OR (d IS NOT NULL AND d.id = :departmentId)) AND " +
           "(:searchTerm IS NULL OR " +
           " LOWER(i.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           " LOWER(i.trackingNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           " (i.address IS NOT NULL AND LOWER(i.address) LIKE LOWER(CONCAT('%', :searchTerm, '%')))) " +
           "ORDER BY i.createdAt DESC, i.id DESC")
    List<Issue> findWithFilters(
            @Param("status") IssueStatus status,
            @Param("category") IssueCategory category,
            @Param("departmentId") Long departmentId,
            @Param("searchTerm") String searchTerm
    );

    long countByStatus(IssueStatus status);

    long countByCategory(IssueCategory category);

    long countByAssignedDepartmentId(Long departmentId);
}
