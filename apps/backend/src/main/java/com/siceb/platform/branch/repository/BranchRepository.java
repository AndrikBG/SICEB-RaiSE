package com.siceb.platform.branch.repository;

import com.siceb.platform.branch.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BranchRepository extends JpaRepository<Branch, UUID> {

    Optional<Branch> findByName(String name);

    List<Branch> findByActiveTrue();

    boolean existsByName(String name);
}
