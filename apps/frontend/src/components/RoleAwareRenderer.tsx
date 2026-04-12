import type { ReactNode } from 'react';
import { useAuthStore } from '@/stores/auth-store';

interface RoleAwareRendererProps {
  permission: string;
  children: ReactNode;
  fallback?: ReactNode;
}

/**
 * Conditional UI rendering based on JWT permissions (T3.7.4).
 * Elements without permission are NOT rendered (not merely hidden).
 * All permission checks are mirrored server-side via @PreAuthorize.
 */
export function RoleAwareRenderer({ permission, children, fallback = null }: RoleAwareRendererProps) {
  const hasPermission = useAuthStore((s) => s.hasPermission);
  return hasPermission(permission) ? <>{children}</> : <>{fallback}</>;
}

interface RequireAnyPermissionProps {
  permissions: string[];
  children: ReactNode;
  fallback?: ReactNode;
}

export function RequireAnyPermission({ permissions, children, fallback = null }: RequireAnyPermissionProps) {
  const user = useAuthStore((s) => s.user);
  const has = permissions.some((p) => user?.permissions?.includes(p));
  return has ? <>{children}</> : <>{fallback}</>;
}
