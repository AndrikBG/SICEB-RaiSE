/**
 * Billing and Payments module — Payment registration for consultations, pharmacy,
 * and laboratory; service tariff configuration with DECIMAL(19,4); receipt generation;
 * future CFDI integration point.
 *
 * <p>Dependencies: none (outgoing). Read-only access to: Clinical Care, Pharmacy, Laboratory.
 * <p>Implementation: Tariff configuration activated in Phase 4 (S4.3).
 * Full billing (payments, receipts, CFDI) deferred to Phase 5.
 */
package com.siceb.domain.billing;
