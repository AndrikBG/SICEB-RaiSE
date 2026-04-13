package com.siceb.domain.inventory.repository;

import com.siceb.domain.inventory.model.InventoryDelta;
import com.siceb.domain.inventory.model.InventoryDeltaId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryDeltaRepository extends JpaRepository<InventoryDelta, InventoryDeltaId> {

    boolean existsByIdempotencyKey(String idempotencyKey);

    Optional<InventoryDelta> findByIdempotencyKey(String idempotencyKey);
}
