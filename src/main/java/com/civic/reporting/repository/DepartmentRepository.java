package com.civic.reporting.repository;

import com.civic.reporting.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    Optional<Department> findByCodeIgnoreCase(String code);
    Optional<Department> findByNameIgnoreCase(String name);
    boolean existsByCode(String code);
}
