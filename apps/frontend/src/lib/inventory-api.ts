import { api } from './api-client';

// ---- Request types ----

export interface IncrementRequest {
  itemId: string;
  quantity: number;
  reason?: string;
  sourceRef?: string;
}

export interface DecrementRequest {
  itemId: string;
  quantity: number;
  reason?: string;
  sourceRef?: string;
}

export interface AdjustRequest {
  itemId: string;
  absoluteQuantity: number;
  reason: string;
}

export interface ThresholdRequest {
  threshold: number;
}

export interface ExpirationRequest {
  expirationDate: string;
}

// ---- Response types ----

export interface DeltaResponse {
  deltaId: string;
  itemId: string;
  deltaType: string;
  quantityChange: number | null;
  absoluteQuantity: number | null;
}

export interface InventoryItemResponse {
  itemId: string;
  branchId: string;
  sku: string;
  name: string;
  category: string;
  serviceId: string;
  currentStock: number;
  minThreshold: number;
  unitOfMeasure: string;
  expirationDate: string | null;
  stockStatus: 'OK' | 'LOW_STOCK' | 'OUT_OF_STOCK';
  expirationStatus: 'OK' | 'EXPIRING_SOON' | 'EXPIRED';
}

export interface InventoryPageResponse {
  content: InventoryItemResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

// ---- Query params ----

export interface InventoryQueryParams {
  filterStatus?: 'OK' | 'LOW_STOCK' | 'OUT_OF_STOCK';
  filterCategory?: string;
  filterService?: string;
  search?: string;
  page?: number;
  size?: number;
}

// ---- API client ----

function idempotencyHeader(key: string): Record<string, string> {
  return { 'Idempotency-Key': key };
}

export const inventoryApi = {
  increment: (data: IncrementRequest, idempotencyKey: string) =>
    api.post<DeltaResponse>('/api/inventory/increments', data, {
      headers: idempotencyHeader(idempotencyKey),
    }),

  decrement: (data: DecrementRequest, idempotencyKey: string) =>
    api.post<DeltaResponse>('/api/inventory/decrements', data, {
      headers: idempotencyHeader(idempotencyKey),
    }),

  adjust: (data: AdjustRequest, idempotencyKey: string) =>
    api.post<DeltaResponse>('/api/inventory/adjustments', data, {
      headers: idempotencyHeader(idempotencyKey),
    }),

  setThreshold: (itemId: string, data: ThresholdRequest, idempotencyKey: string) =>
    api.put<DeltaResponse>(`/api/inventory/${itemId}/threshold`, data, {
      headers: idempotencyHeader(idempotencyKey),
    }),

  updateExpiration: (itemId: string, data: ExpirationRequest, idempotencyKey: string) =>
    api.put<DeltaResponse>(`/api/inventory/${itemId}/expiration`, data, {
      headers: idempotencyHeader(idempotencyKey),
    }),

  list: (params?: InventoryQueryParams) =>
    api.get<InventoryPageResponse>('/api/inventory', { params }),

  get: (itemId: string) =>
    api.get<InventoryItemResponse>(`/api/inventory/${itemId}`),

  exportExcel: () =>
    api.get('/api/inventory/export', { responseType: 'blob' }),
};
