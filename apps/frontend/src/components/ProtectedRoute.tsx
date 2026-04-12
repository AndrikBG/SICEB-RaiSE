import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuthStore } from '@/stores/auth-store';

export function ProtectedRoute() {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const mustChangePassword = useAuthStore((s) => s.mustChangePassword);
  const activeBranch = useAuthStore((s) => s.activeBranch);
  const user = useAuthStore((s) => s.user);
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (mustChangePassword && location.pathname !== '/change-password') {
    return <Navigate to="/change-password" replace />;
  }

  if (!activeBranch && user && (user.branches?.length ?? 0) > 1 && location.pathname !== '/select-branch') {
    return <Navigate to="/select-branch" replace />;
  }

  return <Outlet />;
}
