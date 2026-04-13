import { api } from './api-client';

// ---- Response types ----

export interface BranchResponse {
  branchId: string;
  name: string;
  address: string;
  phone: string | null;
  email: string | null;
  openingTime: string | null;
  closingTime: string | null;
  branchCode: string | null;
  isActive: boolean;
  onboardingComplete: boolean;
}

export interface OnboardingStepResponse {
  name: string;
  status: 'PENDING' | 'COMPLETED' | 'FAILED';
  completedAt: string | null;
}

export interface OnboardingStatusResponse {
  branchId: string;
  steps: OnboardingStepResponse[];
  overallStatus: string;
  completedSteps: number;
  totalSteps: number;
}

export interface BranchSwitchResponse {
  accessToken: string;
  branchId: string;
  branchName: string;
}

// ---- Request types ----

export interface RegisterBranchRequest {
  name: string;
  address?: string;
  phone?: string;
  email?: string;
  openingTime?: string;
  closingTime?: string;
  branchCode?: string;
}

// ---- API client ----

export const branchApi = {
  list: (includeInactive = false) =>
    api.get<BranchResponse[]>('/api/branches', { params: { includeInactive } }),

  get: (branchId: string) =>
    api.get<BranchResponse>(`/api/branches/${branchId}`),

  register: (data: RegisterBranchRequest) =>
    api.post<BranchResponse>('/api/branches', data),

  deactivate: (branchId: string) =>
    api.post<BranchResponse>(`/api/branches/${branchId}/deactivate`),

  getOnboarding: (branchId: string) =>
    api.get<OnboardingStatusResponse>(`/api/branches/${branchId}/onboarding`),

  switchBranch: (branchId: string) =>
    api.post<BranchSwitchResponse>('/api/session/branch', { branchId }),
};
