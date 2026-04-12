import { api } from './api-client';

export interface LoginRequest {
  username: string;
  password: string;
}

export interface BranchInfo {
  id: string;
  name: string;
}

export interface UserInfo {
  id: string;
  username: string;
  fullName: string;
  email: string;
  role: string;
  permissions: string[];
  residencyLevel: string | null;
  staffId: string | null;
  activeBranchId: string;
  branches: BranchInfo[];
}

export interface LoginResponse {
  accessToken: string;
  user: UserInfo;
  mustChangePassword: boolean;
}

export interface RefreshResponse {
  accessToken: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export async function login(data: LoginRequest): Promise<LoginResponse> {
  const res = await api.post<LoginResponse>('/auth/login', data);
  return res.data;
}

export async function refreshToken(): Promise<RefreshResponse> {
  const res = await api.post<RefreshResponse>('/auth/refresh', null, {
    withCredentials: true,
  });
  return res.data;
}

export async function logout(): Promise<void> {
  await api.post('/auth/logout', null, { withCredentials: true });
}

export async function changePassword(data: ChangePasswordRequest): Promise<void> {
  await api.post('/auth/change-password', data);
}

export interface UserListItem {
  id: string;
  username: string;
  fullName: string;
  email: string;
  role: string;
  active: boolean;
  mustChangePassword: boolean;
  primaryBranchId: string;
  branchAssignments: string[];
}

/** Alineado con `RoleResponse` del backend (`GET /api/roles`). */
export interface RoleItem {
  id: string;
  name: string;
  description?: string | null;
  systemRole: boolean;
  active: boolean;
  /** Claves de permiso (strings), no objetos completos. */
  permissions: string[];
}

export interface PermissionItem {
  permissionId: string;
  key: string;
  description: string;
  category: string;
  requiresResidencyCheck: boolean;
}

export async function fetchUsers(): Promise<UserListItem[]> {
  const res = await api.get<UserListItem[]>('/api/users');
  return res.data;
}

export async function fetchRoles(): Promise<RoleItem[]> {
  const res = await api.get<RoleItem[]>('/api/roles');
  return res.data;
}

export async function fetchPermissions(): Promise<PermissionItem[]> {
  const res = await api.get<PermissionItem[]>('/api/permissions');
  return res.data;
}

export async function createUser(data: Record<string, unknown>): Promise<unknown> {
  const res = await api.post('/api/users', data);
  return res.data;
}

export async function updateUser(userId: string, data: Record<string, unknown>): Promise<unknown> {
  const res = await api.put(`/api/users/${userId}`, data);
  return res.data;
}

export async function deactivateUser(userId: string): Promise<void> {
  await api.post(`/api/users/${userId}/deactivate`);
}

export async function activateUser(userId: string): Promise<void> {
  await api.post(`/api/users/${userId}/activate`);
}

export async function createRole(data: {
  name: string;
  description?: string;
  permissionKeys: string[];
}): Promise<unknown> {
  const res = await api.post('/api/roles', data);
  return res.data;
}

export async function updateRole(roleId: string, data: { permissionKeys: string[] }): Promise<unknown> {
  const res = await api.put(`/api/roles/${roleId}`, data);
  return res.data;
}
