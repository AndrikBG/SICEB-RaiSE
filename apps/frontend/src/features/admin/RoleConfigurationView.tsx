import { useState, useEffect, type FormEvent } from 'react';
import {
  fetchRoles,
  fetchPermissions,
  createRole,
  updateRole,
  type RoleItem,
  type PermissionItem,
} from '@/lib/auth-api';

export function RoleConfigurationView() {
  const [roles, setRoles] = useState<RoleItem[]>([]);
  const [permissions, setPermissions] = useState<PermissionItem[]>([]);
  const [selectedRole, setSelectedRole] = useState<RoleItem | null>(null);
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [newRoleName, setNewRoleName] = useState('');
  const [selectedPermissions, setSelectedPermissions] = useState<Set<string>>(new Set());
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  async function loadData() {
    setLoading(true);
    try {
      const [r, p] = await Promise.all([fetchRoles(), fetchPermissions()]);
      setRoles(r);
      setPermissions(p);
    } catch {
      setError('Error al cargar datos');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { loadData(); }, []);

  function handleSelectRole(role: RoleItem) {
    setSelectedRole(role);
    setSelectedPermissions(new Set(role.permissions ?? []));
    setShowCreateForm(false);
    setSuccess('');
  }

  function togglePermission(permId: string) {
    setSelectedPermissions((prev) => {
      const next = new Set(prev);
      if (next.has(permId)) {
        next.delete(permId);
      } else {
        next.add(permId);
      }
      return next;
    });
  }

  async function handleCreate(e: FormEvent) {
    e.preventDefault();
    setError('');
    try {
      await createRole({
        name: newRoleName,
        permissionKeys: Array.from(selectedPermissions),
      });
      setNewRoleName('');
      setSelectedPermissions(new Set());
      setShowCreateForm(false);
      setSuccess('Rol creado exitosamente');
      await loadData();
    } catch {
      setError('Error al crear rol');
    }
  }

  async function handleUpdatePermissions() {
    if (!selectedRole) return;
    setError('');
    try {
      await updateRole(selectedRole.id, {
        permissionKeys: Array.from(selectedPermissions),
      });
      setSuccess('Permisos actualizados');
      await loadData();
      const updated = roles.find((r) => r.id === selectedRole.id);
      if (updated) setSelectedRole(updated);
    } catch {
      setError('Error al actualizar permisos');
    }
  }

  const permissionsByCategory = permissions.reduce<Record<string, PermissionItem[]>>((acc, p) => {
    if (!acc[p.category]) acc[p.category] = [];
    acc[p.category].push(p);
    return acc;
  }, {});

  if (loading) {
    return <div className="flex items-center justify-center py-12 text-gray-500">Cargando...</div>;
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-gray-900">Configuración de Roles</h1>
          <p className="text-sm text-gray-500">
            {roles.length} roles configurados &middot; MNT-03: nuevos roles en &lt;30 min
          </p>
        </div>
        <button
          onClick={() => {
            setShowCreateForm(!showCreateForm);
            setSelectedRole(null);
            setSelectedPermissions(new Set());
          }}
          className="bg-purple-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-purple-700 transition-colors"
        >
          {showCreateForm ? 'Cancelar' : 'Nuevo Rol'}
        </button>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-2 rounded-lg text-sm">{error}</div>
      )}
      {success && (
        <div className="bg-green-50 border border-green-200 text-green-700 px-4 py-2 rounded-lg text-sm">{success}</div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="bg-white border border-gray-200 rounded-xl p-4">
          <h2 className="font-semibold text-gray-900 mb-3">Roles</h2>
          <div className="space-y-1">
            {roles.map((role) => (
              <button
                key={role.id}
                onClick={() => handleSelectRole(role)}
                className={`w-full text-left px-3 py-2 rounded-lg text-sm transition-colors ${
                  selectedRole?.id === role.id
                    ? 'bg-purple-100 text-purple-800 font-medium'
                    : 'hover:bg-gray-50 text-gray-700'
                }`}
              >
                <div className="flex items-center justify-between">
                  <span>{role.name}</span>
                  {role.systemRole && (
                    <span className="text-xs bg-gray-100 text-gray-500 px-1.5 py-0.5 rounded">sistema</span>
                  )}
                </div>
              </button>
            ))}
          </div>
        </div>

        <div className="lg:col-span-2 bg-white border border-gray-200 rounded-xl p-4">
          {showCreateForm ? (
            <form onSubmit={handleCreate} className="space-y-4">
              <h2 className="font-semibold text-gray-900">Crear Nuevo Rol</h2>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Nombre del rol</label>
                <input
                  value={newRoleName}
                  onChange={(e) => setNewRoleName(e.target.value)}
                  required
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-purple-500 focus:outline-none"
                  placeholder="Ej: Nutricionista"
                />
              </div>
              <PermissionGrid
                categories={permissionsByCategory}
                selected={selectedPermissions}
                onToggle={togglePermission}
              />
              <button
                type="submit"
                className="bg-purple-600 text-white px-5 py-2 rounded-lg text-sm font-medium hover:bg-purple-700 transition-colors"
              >
                Crear Rol
              </button>
            </form>
          ) : selectedRole ? (
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <h2 className="font-semibold text-gray-900">
                  Permisos: {selectedRole.name}
                </h2>
                {!selectedRole.systemRole && (
                  <button
                    onClick={handleUpdatePermissions}
                    className="bg-purple-600 text-white px-4 py-1.5 rounded-lg text-sm font-medium hover:bg-purple-700 transition-colors"
                  >
                    Guardar Cambios
                  </button>
                )}
              </div>
              {selectedRole.systemRole && (
                <p className="text-xs text-amber-600 bg-amber-50 px-3 py-2 rounded-lg">
                  Los roles de sistema no pueden ser editados.
                </p>
              )}
              <PermissionGrid
                categories={permissionsByCategory}
                selected={selectedPermissions}
                onToggle={selectedRole.systemRole ? undefined : togglePermission}
              />
            </div>
          ) : (
            <div className="flex items-center justify-center py-12 text-gray-400 text-sm">
              Selecciona un rol para ver sus permisos
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

function PermissionGrid({
  categories,
  selected,
  onToggle,
}: {
  categories: Record<string, PermissionItem[]>;
  selected: Set<string>;
  onToggle?: (id: string) => void;
}) {
  return (
    <div className="space-y-4 max-h-96 overflow-y-auto">
      {Object.entries(categories).map(([category, perms]) => (
        <div key={category}>
          <h3 className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2">
            {category}
          </h3>
          <div className="space-y-1">
            {perms.map((perm) => (
              <label
                key={perm.key}
                className={`flex items-center gap-2 px-2 py-1.5 rounded text-sm ${
                  onToggle ? 'cursor-pointer hover:bg-gray-50' : 'cursor-default'
                }`}
              >
                <input
                  type="checkbox"
                  checked={selected.has(perm.key)}
                  onChange={() => onToggle?.(perm.key)}
                  disabled={!onToggle}
                  className="h-4 w-4 text-purple-600 rounded border-gray-300 focus:ring-purple-500"
                />
                <div>
                  <span className="text-gray-800">{perm.key}</span>
                  <span className="text-gray-400 ml-1">— {perm.description}</span>
                </div>
              </label>
            ))}
          </div>
        </div>
      ))}
    </div>
  );
}
