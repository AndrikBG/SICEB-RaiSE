/**
 * Inventory module — Branch-scoped stock tracking via CQRS delta commands (CRN-43/CRN-44).
 * Append-only delta store with PG trigger materialization, idempotency keys,
 * low-stock alerts, and expiration tracking.
 *
 * <p>Dependencies: none (leaf module). Consumed by: Pharmacy, Supply Chain (incoming).
 * <p>Implementation: Phase 4 (S4.2).
 */
package com.siceb.domain.inventory;
