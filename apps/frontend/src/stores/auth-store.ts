import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import type { UserInfo, BranchInfo } from '@/lib/auth-api';

export interface AuthState {
  user: UserInfo | null;
  accessToken: string | null;
  sessionVersion: number;
  isAuthenticated: boolean;
  mustChangePassword: boolean;
  activeBranch: BranchInfo | null;

  setSession: (user: UserInfo, accessToken: string, mustChangePassword: boolean) => void;
  clearSession: () => void;
  updateToken: (accessToken: string) => void;
  setActiveBranch: (branch: BranchInfo) => void;
  hasPermission: (permission: string) => boolean;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      user: null,
      accessToken: null,
      sessionVersion: 0,
      isAuthenticated: false,
      mustChangePassword: false,
      activeBranch: null,

      setSession: (user, accessToken, mustChangePassword) => {
        const branches = user.branches ?? [];
        const normalized = { ...user, branches };
        return set({
          user: normalized,
          accessToken,
          sessionVersion: get().sessionVersion + 1,
          isAuthenticated: true,
          mustChangePassword,
          activeBranch: branches.length === 1 ? branches[0] : null,
        });
      },

      clearSession: () =>
        set({
          user: null,
          accessToken: null,
          isAuthenticated: false,
          mustChangePassword: false,
          activeBranch: null,
        }),

      updateToken: (accessToken) => set({ accessToken }),

      setActiveBranch: (branch) => set({ activeBranch: branch }),

      hasPermission: (permission: string) => {
        const { user } = get();
        return user?.permissions?.includes(permission) ?? false;
      },
    }),
    {
      name: 'siceb-auth',
      storage: createJSONStorage(() => sessionStorage),
      partialize: (state) => ({
        user: state.user,
        accessToken: state.accessToken,
        isAuthenticated: state.isAuthenticated,
        mustChangePassword: state.mustChangePassword,
        activeBranch: state.activeBranch,
      }),
      merge: (persisted, current) => {
        const p = persisted as Partial<AuthState> | undefined;
        const next = { ...current, ...p } as AuthState;
        if (next.user && next.user.branches == null) {
          next.user = { ...next.user, branches: [] };
        }
        return next;
      },
    },
  ),
);
