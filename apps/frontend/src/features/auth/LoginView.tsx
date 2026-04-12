import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '@/stores/auth-store';
import { login } from '@/lib/auth-api';

export function LoginView() {
  const navigate = useNavigate();
  const setSession = useAuthStore((s) => s.setSession);

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const result = await login({ username, password });
      setSession(result.user, result.accessToken, result.mustChangePassword);

      if (result.mustChangePassword) {
        navigate('/change-password', { replace: true });
      } else if ((result.user.branches?.length ?? 0) > 1) {
        navigate('/select-branch', { replace: true });
      } else {
        navigate('/', { replace: true });
      }
    } catch {
      setError('Credenciales inválidas. Verifica tu usuario y contraseña.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-purple-50 to-indigo-100 px-4">
      <div className="w-full max-w-sm">
        <div className="bg-white rounded-2xl shadow-xl p-8">
          <div className="text-center mb-8">
            <h1 className="text-3xl font-bold text-purple-700">SICEB</h1>
            <p className="text-sm text-gray-500 mt-1">
              Sistema Integral de Clínicas y Expedientes para Bienestar
            </p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-5">
            <div>
              <label htmlFor="username" className="block text-sm font-medium text-gray-700 mb-1">
                Usuario
              </label>
              <input
                id="username"
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                required
                autoComplete="username"
                className="w-full px-3 py-2 border border-gray-300 rounded-lg shadow-sm focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-purple-500 text-sm"
                placeholder="nombre.usuario"
              />
            </div>

            <div>
              <label htmlFor="password" className="block text-sm font-medium text-gray-700 mb-1">
                Contraseña
              </label>
              <input
                id="password"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                autoComplete="current-password"
                className="w-full px-3 py-2 border border-gray-300 rounded-lg shadow-sm focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-purple-500 text-sm"
                placeholder="••••••••"
              />
            </div>

            {error && (
              <div className="bg-red-50 border border-red-200 text-red-700 px-3 py-2 rounded-lg text-sm">
                {error}
              </div>
            )}

            <button
              type="submit"
              disabled={loading || !username || !password}
              className="w-full bg-purple-600 text-white py-2.5 px-4 rounded-lg font-medium text-sm hover:bg-purple-700 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              {loading ? 'Ingresando...' : 'Iniciar sesión'}
            </button>
          </form>
        </div>

        <p className="text-center text-xs text-gray-400 mt-6">
          v3.0 &middot; Datos protegidos bajo LFPDPPP
        </p>
      </div>
    </div>
  );
}
