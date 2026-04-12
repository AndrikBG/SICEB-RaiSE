import { useState, useEffect, type FormEvent } from 'react';
import {
  fetchUsers,
  fetchRoles,
  createUser,
  deactivateUser,
  activateUser,
  type UserListItem,
  type RoleItem,
} from '@/lib/auth-api';
import { useAuthStore } from '@/stores/auth-store';

export function UserManagementView() {
  const [users, setUsers] = useState<UserListItem[]>([]);
  const [roles, setRoles] = useState<RoleItem[]>([]);
  const [showForm, setShowForm] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const userBranches = useAuthStore((s) => s.user?.branches ?? []);

  const [form, setForm] = useState({
    username: '',
    email: '',
    fullName: '',
    roleId: '',
    primaryBranchId: '',
    temporaryPassword: '',
  });

  async function loadData() {
    setLoading(true);
    try {
      const [u, r] = await Promise.all([fetchUsers(), fetchRoles()]);
      setUsers(u);
      setRoles(r);
    } catch {
      setError('Error al cargar datos');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { loadData(); }, []);

  async function handleCreate(e: FormEvent) {
    e.preventDefault();
    setError('');
    try {
      await createUser({
        username: form.username,
        email: form.email,
        fullName: form.fullName,
        roleId: form.roleId,
        primaryBranchId: form.primaryBranchId,
        temporaryPassword: form.temporaryPassword || undefined,
      });
      setShowForm(false);
      setForm({ username: '', email: '', fullName: '', roleId: '', primaryBranchId: '', temporaryPassword: '' });
      await loadData();
    } catch (err: unknown) {
      const axiosErr = err as { response?: { status?: number; data?: { message?: string } } };
      if (axiosErr.response?.status === 400) {
        setError(axiosErr.response.data?.message ?? 'Datos inválidos. Verifica los campos e intenta de nuevo.');
      } else if (axiosErr.response?.status === 409) {
        setError('El nombre de usuario o email ya existe.');
      } else {
        setError('Error inesperado al crear usuario. Intenta de nuevo más tarde.');
      }
    }
  }

  async function handleToggleActive(user: UserListItem) {
    try {
      if (user.active) {
        await deactivateUser(user.id);
      } else {
        await activateUser(user.id);
      }
      await loadData();
    } catch {
      setError('Error al cambiar estado del usuario');
    }
  }

  if (loading) {
    return <div className="flex items-center justify-center py-12 text-gray-500">Cargando...</div>;
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-gray-900">Gestión de Usuarios</h1>
          <p className="text-sm text-gray-500">{users.length} usuarios registrados</p>
        </div>
        <button
          onClick={() => setShowForm(!showForm)}
          className="bg-purple-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-purple-700 transition-colors"
        >
          {showForm ? 'Cancelar' : 'Nuevo Usuario'}
        </button>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-2 rounded-lg text-sm">
          {error}
        </div>
      )}

      {showForm && (
        <form onSubmit={handleCreate} autoComplete="off" className="bg-white border border-gray-200 rounded-xl p-6 space-y-4">
          <h2 className="font-semibold text-gray-900">Crear Usuario</h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Nombre completo</label>
              <input
                value={form.fullName}
                onChange={(e) => setForm({ ...form, fullName: e.target.value })}
                required
                autoComplete="off"
                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-purple-500 focus:outline-none"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Usuario</label>
              <input
                value={form.username}
                onChange={(e) => setForm({ ...form, username: e.target.value })}
                required
                autoComplete="off"
                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-purple-500 focus:outline-none"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Email</label>
              <input
                type="email"
                value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
                required
                autoComplete="off"
                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-purple-500 focus:outline-none"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Rol</label>
              <select
                value={form.roleId}
                onChange={(e) => setForm({ ...form, roleId: e.target.value })}
                required
                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-purple-500 focus:outline-none"
              >
                <option value="">Seleccionar rol...</option>
                {roles.map((r) => (
                  <option key={r.id} value={r.id}>{r.name}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Sucursal principal</label>
              <select
                value={form.primaryBranchId}
                onChange={(e) => setForm({ ...form, primaryBranchId: e.target.value })}
                required
                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-purple-500 focus:outline-none"
              >
                <option value="">Seleccionar sucursal...</option>
                {userBranches.map((b) => (
                  <option key={b.id} value={b.id}>{b.name}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Contraseña temporal</label>
              <input
                type="password"
                value={form.temporaryPassword}
                onChange={(e) => setForm({ ...form, temporaryPassword: e.target.value })}
                minLength={8}
                autoComplete="new-password"
                placeholder="Dejar vacío para usar la predeterminada"
                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-purple-500 focus:outline-none"
              />
            </div>
          </div>
          <div className="flex justify-end">
            <button
              type="submit"
              className="bg-purple-600 text-white px-5 py-2 rounded-lg text-sm font-medium hover:bg-purple-700 transition-colors"
            >
              Crear
            </button>
          </div>
        </form>
      )}

      <div className="bg-white border border-gray-200 rounded-xl overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 border-b border-gray-200">
            <tr>
              <th className="px-4 py-3 text-left font-medium text-gray-600">Nombre</th>
              <th className="px-4 py-3 text-left font-medium text-gray-600">Usuario</th>
              <th className="px-4 py-3 text-left font-medium text-gray-600">Email</th>
              <th className="px-4 py-3 text-left font-medium text-gray-600">Rol</th>
              <th className="px-4 py-3 text-left font-medium text-gray-600">Estado</th>
              <th className="px-4 py-3 text-right font-medium text-gray-600">Acciones</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {users.map((u) => (
              <tr key={u.id} className="hover:bg-gray-50">
                <td className="px-4 py-3 font-medium text-gray-900">{u.fullName}</td>
                <td className="px-4 py-3 text-gray-600">{u.username}</td>
                <td className="px-4 py-3 text-gray-600">{u.email}</td>
                <td className="px-4 py-3 text-gray-600">{u.role || 'Sin rol'}</td>
                <td className="px-4 py-3">
                  <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium ${
                    u.active ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'
                  }`}>
                    {u.active ? 'Activo' : 'Inactivo'}
                  </span>
                </td>
                <td className="px-4 py-3 text-right">
                  <button
                    onClick={() => handleToggleActive(u)}
                    className={`text-xs font-medium px-3 py-1 rounded-md transition-colors ${
                      u.active
                        ? 'text-red-600 hover:bg-red-50'
                        : 'text-green-600 hover:bg-green-50'
                    }`}
                  >
                    {u.active ? 'Desactivar' : 'Activar'}
                  </button>
                </td>
              </tr>
            ))}
            {users.length === 0 && (
              <tr>
                <td colSpan={6} className="px-4 py-8 text-center text-gray-400">
                  No hay usuarios registrados
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
