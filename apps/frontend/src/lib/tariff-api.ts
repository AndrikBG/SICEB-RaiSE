import { api } from './api-client';

// ---- Response types ----

export interface TariffResponse {
  tariffId: string;
  serviceId: string;
  branchId: string;
  basePrice: number;
  effectiveFrom: string;
  createdBy: string;
  createdAt: string;
}

export interface TariffPageResponse {
  content: TariffResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

// ---- Request types ----

export interface CreateTariffRequest {
  serviceId: string;
  basePrice: number;
  effectiveFrom: string;
}

export interface UpdateTariffRequest {
  basePrice: number;
  effectiveFrom: string;
}

// ---- API client ----

export const tariffApi = {
  create: (data: CreateTariffRequest) =>
    api.post<TariffResponse>('/api/tariffs', data),

  update: (tariffId: string, data: UpdateTariffRequest) =>
    api.put<TariffResponse>(`/api/tariffs/${tariffId}`, data),

  getActive: (serviceId: string) =>
    api.get<TariffResponse>('/api/tariffs/active', { params: { serviceId } }),

  list: (params?: {
    serviceId?: string;
    includeHistorical?: boolean;
    page?: number;
    size?: number;
  }) => api.get<TariffPageResponse>('/api/tariffs', { params }),

  search: (query: string, page = 0, size = 20) =>
    api.get<TariffPageResponse>('/api/tariffs/search', {
      params: { query, page, size },
    }),
};
