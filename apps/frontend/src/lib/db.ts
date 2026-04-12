import Dexie, { type EntityTable } from 'dexie';

/**
 * All tenant-scoped tables include `branchId` for offline multi-tenant isolation.
 * Queries MUST always filter by branchId — helper methods enforce this.
 */

export interface SyncQueueEntry {
  id?: number;
  branchId: string;
  entityType: string;
  entityId: string;
  action: 'create' | 'update' | 'delete';
  payload: unknown;
  idempotencyKey: string;
  createdAt: string;
  syncedAt?: string;
  status: 'pending' | 'syncing' | 'synced' | 'failed';
  retryCount: number;
  errorMessage?: string;
}

export interface CachedEntity {
  id?: number;
  branchId: string;
  entityType: string;
  entityId: string;
  data: unknown;
  updatedAt: string;
  version: number;
}

export interface AppSetting {
  key: string;
  value: unknown;
}

class SicebDatabase extends Dexie {
  syncQueue!: EntityTable<SyncQueueEntry, 'id'>;
  cachedEntities!: EntityTable<CachedEntity, 'id'>;
  appSettings!: EntityTable<AppSetting, 'key'>;

  constructor() {
    super('siceb');

    this.version(1).stores({
      syncQueue:
        '++id, branchId, [branchId+entityType], [branchId+status], idempotencyKey, createdAt',
      cachedEntities:
        '++id, branchId, [branchId+entityType], [branchId+entityType+entityId], &[entityType+entityId]',
      appSettings: 'key',
    });
  }
}

export const db = new SicebDatabase();

export function scopedSyncQueue(branchId: string) {
  return db.syncQueue.where('branchId').equals(branchId);
}

export function scopedCachedEntities(branchId: string, entityType?: string) {
  if (entityType) {
    return db.cachedEntities
      .where('[branchId+entityType]')
      .equals([branchId, entityType]);
  }
  return db.cachedEntities.where('branchId').equals(branchId);
}

export function pendingSyncCount(branchId: string) {
  return db.syncQueue
    .where('[branchId+status]')
    .equals([branchId, 'pending'])
    .count();
}
