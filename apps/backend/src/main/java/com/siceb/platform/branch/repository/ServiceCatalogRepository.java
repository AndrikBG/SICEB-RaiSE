package com.siceb.platform.branch.repository;

import com.siceb.platform.branch.entity.ServiceCatalogEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ServiceCatalogRepository extends JpaRepository<ServiceCatalogEntry, UUID> {

    List<ServiceCatalogEntry> findByBranchIdAndActiveTrue(UUID branchId);
}
