import type { StompSubscription } from '@stomp/stompjs';
import { subscribeTopic } from './ws-client';
import { useAuthStore } from '@/stores/auth-store';

export interface InventoryChangeEvent {
  branchId: string;
  itemId: string;
  deltaType: 'INCREMENT' | 'DECREMENT' | 'ADJUST' | 'THRESHOLD' | 'EXPIRATION';
  newStock: number;
  stockStatus: 'OK' | 'LOW_STOCK' | 'OUT_OF_STOCK';
  timestamp: number;
}

export type InventoryUpdateCallback = (event: InventoryChangeEvent) => void;

let currentSubscription: StompSubscription | undefined;
let currentBranchId: string | null = null;
let currentCallback: InventoryUpdateCallback | null = null;
let storeUnsubscribe: (() => void) | null = null;

/**
 * Subscribe to real-time inventory updates for a branch.
 * Automatically resubscribes when the active branch changes.
 */
export function subscribeInventory(
  branchId: string,
  onUpdate: InventoryUpdateCallback,
): void {
  // Clean up previous subscription if any
  unsubscribeInventory();

  currentCallback = onUpdate;
  activateSubscription(branchId);

  // Watch for branch switches — resubscribe automatically
  storeUnsubscribe = useAuthStore.subscribe((state) => {
    const newBranchId = state.activeBranch?.id ?? null;
    if (newBranchId && newBranchId !== currentBranchId && currentCallback) {
      activateSubscription(newBranchId);
    }
  });
}

/**
 * Unsubscribe from inventory updates and stop watching branch changes.
 */
export function unsubscribeInventory(): void {
  if (currentSubscription) {
    currentSubscription.unsubscribe();
    currentSubscription = undefined;
  }
  if (storeUnsubscribe) {
    storeUnsubscribe();
    storeUnsubscribe = null;
  }
  currentBranchId = null;
  currentCallback = null;
}

function activateSubscription(branchId: string): void {
  // Unsubscribe from previous branch if switching
  if (currentSubscription) {
    currentSubscription.unsubscribe();
    currentSubscription = undefined;
  }

  currentBranchId = branchId;
  const destination = `/topic/branch/${branchId}/inventory`;

  currentSubscription = subscribeTopic(destination, (message) => {
    if (!currentCallback) return;

    try {
      const event: InventoryChangeEvent = JSON.parse(message.body);
      currentCallback(event);
    } catch {
      console.error('[Inventory WS] Failed to parse message:', message.body);
    }
  });

  if (currentSubscription) {
    console.info(`[Inventory WS] Subscribed to ${destination}`);
  }
}
