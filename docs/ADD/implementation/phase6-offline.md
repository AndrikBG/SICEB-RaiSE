# Phase 6 — Offline-First Architecture and Synchronization

> **Status:** 🔲 Not started  
> **Drivers:** US-076, REL-01, REL-02, USA-01, REL-04, CRN-21, CRN-34, CRN-36, CRN-38, CRN-05, CRN-16, CRN-39  
> **Depends on:** Phases 4 and 5 complete

**Goal:** Enable full PWA offline operation, transparent mode transitions, reliable synchronization with conflict resolution, partial failure recovery, and offline business-rule enforcement.

---

## A6.1 — Service Worker offline strategy

- [ ] **T6.1.1** Workbox cache strategies for static assets and API responses  
- [ ] **T6.1.2** Request interception for transparent offline operation  
- [ ] **T6.1.3** Background Sync for automatic reconnection  
- [ ] **T6.1.4** PWA lifecycle: install and update  

## A6.2 — Local storage and sync queue

- [ ] **T6.2.1** Data subset in IndexedDB (Dexie.js) per active branch (CRN-36)  
- [ ] **T6.2.2** Sync queue with ordering guarantees and duplicate prevention (CRN-34)  
- [ ] **T6.2.3** Cache corruption detection via checksums (REL-04)  
- [ ] **T6.2.4** Cache isolation by `branch_id` when switching branch  

## A6.3 — Synchronization protocol

- [ ] **T6.3.1** Queue-based sync with ordering guarantees  
- [ ] **T6.3.2** Idempotency: 100% offline records synced, zero loss, zero duplicates (REL-01)  
- [ ] **T6.3.3** Partial failure recovery: resume from exact cutover point (REL-02)  
- [ ] **T6.3.4** Concurrent offline inventory conflict resolution (CRN-35)  
- [ ] **T6.3.5** Offline UUID generation without collisions (CRN-38)  

## A6.4 — Offline business rules

- [ ] **T6.4.1** R1/R2 supervisor validation against cached data (CRN-16)  
- [ ] **T6.4.2** Local authorization using cached JWT claims  
- [ ] **T6.4.3** Backward-compatible schemas during sync (CRN-05)  

## A6.5 — Offline UX (CRN-39)

- [ ] **T6.5.1** Visual online/offline indicators  
- [ ] **T6.5.2** Transparent transition in under 3 seconds (USA-01)  
- [ ] **T6.5.3** Sync status and data freshness indicators  

---

## Deliverables

- [ ] **E6.1** Offline-first Service Worker — Cache, interception, Background Sync  
- [ ] **E6.2** Local storage — Branch-scoped IndexedDB, sync queue, checksums  
- [ ] **E6.3** Sync protocol — Zero loss (REL-01), partial resume (REL-02), conflicts  
- [ ] **E6.4** Offline rules — Local auth, supervision, backward compatibility  
- [ ] **E6.5** Offline UX — Indicators, &lt;3s transition, sync state  
- [ ] **E6.6** E2E tests — Disconnect, reconnect, conflicts, partial failures  

---

## Notes and decisions

<!-- Record decisions, issues, and resolutions during this phase. -->
