import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '@/stores/auth-store';
import { changePassword } from '@/lib/auth-api';

export function ChangePasswordView() {
  const navigate = useNavigate();
  const clearSession = useAuthStore((s) => s.clearSession);

  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError('');

    if (newPassword !== confirmPassword) {
      setError('Las contraseñas no coinciden.');
      return;
    }

    if (newPassword.length < 8) {
      setError('La contraseña debe tener al menos 8 caracteres.');
      return;
    }

    setLoading(true);
    try {
      await changePassword({ currentPassword, newPassword });
      clearSession();
      navigate('/login', { replace: true });
    } catch {
      setError('No se pudo cambiar la contraseña. Verifica la contraseña actual.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-purple-50 to-indigo-100 px-4">
      <div className="w-full max-w-sm">
        <div className="bg-white rounded-2xl shadow-xl p-8">
          <div className="text-center mb-6">
            <h1 className="text-2xl font-bold text-gray-900">Cambiar Contraseña</h1>
            <p className="text-sm text-gray-500 mt-1">
              Debes cambiar tu contraseña temporal antes de continuar.
            </p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label htmlFor="current" className="block text-sm font-medium text-gray-700 mb-1">
                Contraseña actual
              </label>
              <input
                id="current"
                type="password"
                value={currentPassword}
                onChange={(e) => setCurrentPassword(e.target.value)}
                required
                className="w-full px-3 py-2 border border-gray-300 rounded-lg shadow-sm focus:outline-none focus:ring-2 focus:ring-purple-500 text-sm"
              />
            </div>

            <div>
              <label htmlFor="newpw" className="block text-sm font-medium text-gray-700 mb-1">
                Nueva contraseña
              </label>
              <input
                id="newpw"
                type="password"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                required
                minLength={8}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg shadow-sm focus:outline-none focus:ring-2 focus:ring-purple-500 text-sm"
              />
            </div>

            <div>
              <label htmlFor="confirm" className="block text-sm font-medium text-gray-700 mb-1">
                Confirmar nueva contraseña
              </label>
              <input
                id="confirm"
                type="password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                required
                className="w-full px-3 py-2 border border-gray-300 rounded-lg shadow-sm focus:outline-none focus:ring-2 focus:ring-purple-500 text-sm"
              />
            </div>

            {error && (
              <div className="bg-red-50 border border-red-200 text-red-700 px-3 py-2 rounded-lg text-sm">
                {error}
              </div>
            )}

            <button
              type="submit"
              disabled={loading}
              className="w-full bg-purple-600 text-white py-2.5 px-4 rounded-lg font-medium text-sm hover:bg-purple-700 disabled:opacity-50 transition-colors"
            >
              {loading ? 'Guardando...' : 'Cambiar contraseña'}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
