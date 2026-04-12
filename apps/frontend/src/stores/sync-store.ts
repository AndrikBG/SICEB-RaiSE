import { create } from 'zustand';

export type ConnectionStatus = 'online' | 'offline' | 'reconnecting';

interface SyncState {
  connectionStatus: ConnectionStatus;
  pendingChanges: number;
  lastSyncAt: string | null;
  isSyncing: boolean;

  setConnectionStatus: (status: ConnectionStatus) => void;
  setPendingChanges: (count: number) => void;
  setLastSyncAt: (timestamp: string) => void;
  setSyncing: (syncing: boolean) => void;
}

export const useSyncStore = create<SyncState>()((set) => ({
  connectionStatus: navigator.onLine ? 'online' : 'offline',
  pendingChanges: 0,
  lastSyncAt: null,
  isSyncing: false,

  setConnectionStatus: (connectionStatus) => set({ connectionStatus }),
  setPendingChanges: (pendingChanges) => set({ pendingChanges }),
  setLastSyncAt: (lastSyncAt) => set({ lastSyncAt }),
  setSyncing: (isSyncing) => set({ isSyncing }),
}));
