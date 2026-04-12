import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '@/stores/auth-store';
import type { BranchInfo } from '@/lib/auth-api';

export function BranchSelectionView() {
  const navigate = useNavigate();
  const user = useAuthStore((s) => s.user);
  const setActiveBranch = useAuthStore((s) => s.setActiveBranch);

  if (!user) {
    navigate('/login', { replace: true });
    return null;
  }

  function handleSelect(branch: BranchInfo) {
    setActiveBranch(branch);
    navigate('/', { replace: true });
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-purple-50 to-indigo-100 px-4">
      <div className="w-full max-w-md">
        <div className="bg-white rounded-2xl shadow-xl p-8">
          <div className="text-center mb-6">
            <h1 className="text-2xl font-bold text-gray-900">Seleccionar Sucursal</h1>
            <p className="text-sm text-gray-500 mt-1">
              Hola, {user.fullName}. Selecciona tu sucursal activa.
            </p>
          </div>

          <div className="space-y-3">
            {(user.branches ?? []).map((branch) => (
              <button
                key={branch.id}
                onClick={() => handleSelect(branch)}
                className="w-full flex items-center gap-3 px-4 py-3 border border-gray-200 rounded-xl hover:border-purple-400 hover:bg-purple-50 transition-colors text-left group"
              >
                <span className="flex-shrink-0 h-10 w-10 bg-purple-100 text-purple-600 rounded-lg flex items-center justify-center font-bold text-sm group-hover:bg-purple-200">
                  {branch.name.charAt(0)}
                </span>
                <span className="font-medium text-gray-800 text-sm">{branch.name}</span>
              </button>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
