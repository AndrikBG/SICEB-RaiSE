import { useState, useEffect, type FormEvent } from 'react';
import {
  branchApi,
  type BranchResponse,
  type OnboardingStatusResponse,
} from '@/lib/branch-api';

export function BranchManagementView() {
  const [branches, setBranches] = useState<BranchResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [showForm, setShowForm] = useState(false);
  const [expandedOnboarding, setExpandedOnboarding] = useState<string | null>(null);
  const [onboardingData, setOnboardingData] = useState<OnboardingStatusResponse | null>(null);

  const [form, setForm] = useState({
    name: '',
    address: '',
    phone: '',
    email: '',
    branchCode: '',
  });

  async function loadBranches() {
    setLoading(true);
    try {
      const res = await branchApi.list(true);
      setBranches(res.data);
    } catch {
      setError('Error al cargar sucursales');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { loadBranches(); }, []);

  async function handleRegister(e: FormEvent) {
    e.preventDefault();
    setError('');
    try {
      await branchApi.register({
        name: form.name,
        address: form.address || undefined,
        phone: form.phone || undefined,
        email: form.email || undefined,
        branchCode: form.branchCode || undefined,
      });
      setShowForm(false);
      setForm({ name: '', address: '', phone: '', email: '', branchCode: '' });
      await loadBranches();
    } catch {
      setError('Error al registrar sucursal');
    }
  }

  async function handleDeactivate(branchId: string) {
    if (!confirm('¿Desactivar esta sucursal?')) return;
    setError('');
    try {
      await branchApi.deactivate(branchId);
      await loadBranches();
    } catch {
      setError('Error al desactivar sucursal');
    }
  }

  async function toggleOnboarding(branchId: string) {
    if (expandedOnboarding === branchId) {
      setExpandedOnboarding(null);
      setOnboardingData(null);
      return;
    }
    try {
      const res = await branchApi.getOnboarding(branchId);
      setOnboardingData(res.data);
      setExpandedOnboarding(branchId);
    } catch {
      setError('Error al cargar onboarding');
    }
  }

  if (loading) {
    return <p className="text-gray-500 text-sm">Cargando sucursales...</p>;
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-xl font-bold text-gray-900">Gestión de Sucursales</h1>
        <button
          onClick={() => setShowForm(!showForm)}
          className="px-4 py-2 bg-teal-600 text-white text-sm font-medium rounded-lg hover:bg-teal-700 transition-colors"
        >
          {showForm ? 'Cancelar' : 'Nueva Sucursal'}
        </button>
      </div>

      {error && (
        <div className="mb-4 p-3 bg-red-50 text-red-700 text-sm rounded-lg">{error}</div>
      )}

      {showForm && (
        <form onSubmit={handleRegister} className="mb-6 bg-white rounded-xl border border-gray-200 p-6 space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">Nombre *</label>
              <input
                required
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-teal-500 focus:border-transparent"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">Código</label>
              <input
                value={form.branchCode}
                onChange={(e) => setForm({ ...form, branchCode: e.target.value })}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-teal-500 focus:border-transparent"
              />
            </div>
            <div className="col-span-2">
              <label className="block text-xs font-medium text-gray-600 mb-1">Dirección</label>
              <input
                value={form.address}
                onChange={(e) => setForm({ ...form, address: e.target.value })}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-teal-500 focus:border-transparent"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">Teléfono</label>
              <input
                value={form.phone}
                onChange={(e) => setForm({ ...form, phone: e.target.value })}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-teal-500 focus:border-transparent"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">Email</label>
              <input
                type="email"
                value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-teal-500 focus:border-transparent"
              />
            </div>
          </div>
          <button
            type="submit"
            className="px-4 py-2 bg-teal-600 text-white text-sm font-medium rounded-lg hover:bg-teal-700 transition-colors"
          >
            Registrar
          </button>
        </form>
      )}

      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 border-b border-gray-200">
            <tr>
              <th className="text-left px-4 py-3 font-medium text-gray-600">Nombre</th>
              <th className="text-left px-4 py-3 font-medium text-gray-600">Código</th>
              <th className="text-left px-4 py-3 font-medium text-gray-600">Dirección</th>
              <th className="text-left px-4 py-3 font-medium text-gray-600">Estado</th>
              <th className="text-left px-4 py-3 font-medium text-gray-600">Onboarding</th>
              <th className="text-right px-4 py-3 font-medium text-gray-600">Acciones</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {branches.map((branch) => (
              <>
                <tr key={branch.branchId} className="hover:bg-gray-50">
                  <td className="px-4 py-3 font-medium text-gray-900">{branch.name}</td>
                  <td className="px-4 py-3 text-gray-600">{branch.branchCode ?? '—'}</td>
                  <td className="px-4 py-3 text-gray-600">{branch.address ?? '—'}</td>
                  <td className="px-4 py-3">
                    <span className={`inline-flex px-2 py-0.5 rounded-full text-xs font-medium ${
                      branch.isActive
                        ? 'bg-green-100 text-green-700'
                        : 'bg-gray-100 text-gray-500'
                    }`}>
                      {branch.isActive ? 'Activa' : 'Inactiva'}
                    </span>
                  </td>
                  <td className="px-4 py-3">
                    <button
                      onClick={() => toggleOnboarding(branch.branchId)}
                      className="text-xs text-teal-600 hover:text-teal-800 underline"
                    >
                      {branch.onboardingComplete ? 'Completo' : 'Ver progreso'}
                    </button>
                  </td>
                  <td className="px-4 py-3 text-right">
                    {branch.isActive && (
                      <button
                        onClick={() => handleDeactivate(branch.branchId)}
                        className="text-xs text-red-600 hover:text-red-800"
                      >
                        Desactivar
                      </button>
                    )}
                  </td>
                </tr>
                {expandedOnboarding === branch.branchId && onboardingData && (
                  <tr key={`${branch.branchId}-onboarding`}>
                    <td colSpan={6} className="px-4 py-3 bg-gray-50">
                      <div className="flex items-center gap-4 text-xs">
                        <span className="font-medium text-gray-600">
                          Progreso: {onboardingData.completedSteps}/{onboardingData.totalSteps}
                        </span>
                        <div className="flex gap-2">
                          {onboardingData.steps.map((step) => (
                            <span
                              key={step.name}
                              className={`px-2 py-1 rounded text-xs ${
                                step.status === 'COMPLETED'
                                  ? 'bg-green-100 text-green-700'
                                  : step.status === 'FAILED'
                                    ? 'bg-red-100 text-red-700'
                                    : 'bg-yellow-100 text-yellow-700'
                              }`}
                            >
                              {step.name}
                            </span>
                          ))}
                        </div>
                      </div>
                    </td>
                  </tr>
                )}
              </>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
