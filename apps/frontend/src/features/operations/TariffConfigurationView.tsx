import { useState, useEffect, useCallback, type FormEvent } from 'react';
import {
  tariffApi,
  type TariffResponse,
  type CreateTariffRequest,
  type UpdateTariffRequest,
} from '@/lib/tariff-api';

export function TariffConfigurationView() {
  const [tariffs, setTariffs] = useState<TariffResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [includeHistorical, setIncludeHistorical] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [isSearching, setIsSearching] = useState(false);

  const [showCreateForm, setShowCreateForm] = useState(false);
  const [showUpdateForm, setShowUpdateForm] = useState<string | null>(null);
  const [formError, setFormError] = useState('');

  const [createForm, setCreateForm] = useState({
    serviceId: '',
    basePrice: '',
    effectiveFrom: '',
  });

  const [updateForm, setUpdateForm] = useState({
    basePrice: '',
    effectiveFrom: '',
  });

  const loadTariffs = useCallback(async (pageNum: number) => {
    setLoading(true);
    setError('');
    try {
      if (isSearching && searchQuery) {
        const res = await tariffApi.search(searchQuery, pageNum, 20);
        setTariffs(res.data.content);
        setTotalPages(res.data.totalPages);
        setTotalElements(res.data.totalElements);
      } else {
        const res = await tariffApi.list({ includeHistorical, page: pageNum, size: 20 });
        setTariffs(res.data.content);
        setTotalPages(res.data.totalPages);
        setTotalElements(res.data.totalElements);
      }
    } catch {
      setError('Error al cargar tarifas');
    } finally {
      setLoading(false);
    }
  }, [isSearching, searchQuery, includeHistorical]);

  useEffect(() => { loadTariffs(page); }, [page, loadTariffs]);

  function handleSearch() {
    if (searchQuery.trim()) {
      setIsSearching(true);
      setPage(0);
      loadTariffs(0);
    } else {
      setIsSearching(false);
      setPage(0);
      loadTariffs(0);
    }
  }

  function clearSearch() {
    setSearchQuery('');
    setIsSearching(false);
    setPage(0);
    loadTariffs(0);
  }

  function validatePrice(price: string): string | null {
    const num = Number(price);
    if (isNaN(num)) return 'El precio debe ser un número válido';
    if (num < 0) return 'El precio no puede ser negativo';
    return null;
  }

  function validateEffectiveFrom(dateStr: string): string | null {
    if (!dateStr) return 'La fecha es requerida';
    return null;
  }

  async function handleCreate(e: FormEvent) {
    e.preventDefault();
    setFormError('');

    const priceErr = validatePrice(createForm.basePrice);
    if (priceErr) { setFormError(priceErr); return; }

    const dateErr = validateEffectiveFrom(createForm.effectiveFrom);
    if (dateErr) { setFormError(dateErr); return; }

    if (!createForm.serviceId.trim()) { setFormError('El servicio es requerido'); return; }

    const data: CreateTariffRequest = {
      serviceId: createForm.serviceId,
      basePrice: Number(createForm.basePrice),
      effectiveFrom: new Date(createForm.effectiveFrom).toISOString(),
    };

    try {
      await tariffApi.create(data);
      setShowCreateForm(false);
      setCreateForm({ serviceId: '', basePrice: '', effectiveFrom: '' });
      await loadTariffs(page);
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setFormError(msg ?? 'Error al crear tarifa');
    }
  }

  async function handleUpdate(tariffId: string, e: FormEvent) {
    e.preventDefault();
    setFormError('');

    const priceErr = validatePrice(updateForm.basePrice);
    if (priceErr) { setFormError(priceErr); return; }

    const dateErr = validateEffectiveFrom(updateForm.effectiveFrom);
    if (dateErr) { setFormError(dateErr); return; }

    const data: UpdateTariffRequest = {
      basePrice: Number(updateForm.basePrice),
      effectiveFrom: new Date(updateForm.effectiveFrom).toISOString(),
    };

    try {
      await tariffApi.update(tariffId, data);
      setShowUpdateForm(null);
      setUpdateForm({ basePrice: '', effectiveFrom: '' });
      await loadTariffs(page);
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setFormError(msg ?? 'Error al actualizar tarifa');
    }
  }

  function startUpdate(tariff: TariffResponse) {
    setUpdateForm({
      basePrice: String(tariff.basePrice),
      effectiveFrom: '',
    });
    setFormError('');
    setShowUpdateForm(tariff.tariffId);
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-xl font-bold text-gray-900">Configuración de Tarifas</h1>
          <p className="text-sm text-gray-500">{totalElements} tarifas</p>
        </div>
        <button
          onClick={() => { setShowCreateForm(!showCreateForm); setFormError(''); }}
          className="px-4 py-2 bg-teal-600 text-white text-sm font-medium rounded-lg hover:bg-teal-700 transition-colors"
        >
          {showCreateForm ? 'Cancelar' : 'Nueva Tarifa'}
        </button>
      </div>

      {error && (
        <div className="mb-4 p-3 bg-red-50 text-red-700 text-sm rounded-lg">{error}</div>
      )}

      {/* Create form */}
      {showCreateForm && (
        <form onSubmit={handleCreate} className="mb-6 bg-white rounded-xl border border-gray-200 p-6 space-y-4">
          <h2 className="text-sm font-semibold text-gray-700">Crear Tarifa</h2>
          {formError && (
            <div className="p-2 bg-red-50 text-red-700 text-xs rounded">{formError}</div>
          )}
          <div className="grid grid-cols-3 gap-4">
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">Servicio (ID) *</label>
              <input
                required
                value={createForm.serviceId}
                onChange={(e) => setCreateForm({ ...createForm, serviceId: e.target.value })}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-teal-500 focus:border-transparent"
                placeholder="UUID del servicio"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">Precio Base *</label>
              <input
                required
                type="number"
                step="0.01"
                min="0"
                value={createForm.basePrice}
                onChange={(e) => setCreateForm({ ...createForm, basePrice: e.target.value })}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-teal-500 focus:border-transparent"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">Vigente Desde *</label>
              <input
                required
                type="date"
                value={createForm.effectiveFrom}
                onChange={(e) => setCreateForm({ ...createForm, effectiveFrom: e.target.value })}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-teal-500 focus:border-transparent"
              />
            </div>
          </div>
          <button
            type="submit"
            className="px-4 py-2 bg-teal-600 text-white text-sm font-medium rounded-lg hover:bg-teal-700 transition-colors"
          >
            Crear
          </button>
        </form>
      )}

      {/* Search + Filters */}
      <div className="mb-4 flex gap-3 items-center">
        <div className="flex gap-1">
          <input
            placeholder="Buscar por nombre de servicio..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
            className="px-3 py-2 border border-gray-300 rounded-lg text-sm w-72"
          />
          <button onClick={handleSearch} className="px-3 py-2 bg-gray-100 hover:bg-gray-200 rounded-lg text-sm">
            Buscar
          </button>
          {isSearching && (
            <button onClick={clearSearch} className="px-3 py-2 text-sm text-gray-500 hover:text-gray-700">
              Limpiar
            </button>
          )}
        </div>
        <label className="flex items-center gap-2 text-sm text-gray-600 ml-auto">
          <input
            type="checkbox"
            checked={includeHistorical}
            onChange={(e) => { setIncludeHistorical(e.target.checked); setPage(0); }}
            className="rounded"
          />
          Incluir historial
        </label>
      </div>

      {/* Table */}
      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 border-b border-gray-200">
            <tr>
              <th className="text-left px-4 py-3 font-medium text-gray-600">Servicio</th>
              <th className="text-right px-4 py-3 font-medium text-gray-600">Precio Base</th>
              <th className="text-left px-4 py-3 font-medium text-gray-600">Vigente Desde</th>
              <th className="text-left px-4 py-3 font-medium text-gray-600">Creado</th>
              <th className="text-right px-4 py-3 font-medium text-gray-600">Acciones</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {loading ? (
              <tr><td colSpan={5} className="px-4 py-8 text-center text-gray-400">Cargando...</td></tr>
            ) : tariffs.length === 0 ? (
              <tr><td colSpan={5} className="px-4 py-8 text-center text-gray-400">Sin tarifas</td></tr>
            ) : (
              tariffs.map((tariff) => (
                <>
                  <tr key={tariff.tariffId} className="hover:bg-gray-50">
                    <td className="px-4 py-3 font-mono text-xs text-gray-600">{tariff.serviceId.slice(0, 8)}...</td>
                    <td className="px-4 py-3 text-right font-medium">
                      ${Number(tariff.basePrice).toFixed(2)}
                    </td>
                    <td className="px-4 py-3 text-gray-600">
                      {new Date(tariff.effectiveFrom).toLocaleDateString('es-MX')}
                    </td>
                    <td className="px-4 py-3 text-gray-500 text-xs">
                      {new Date(tariff.createdAt).toLocaleDateString('es-MX')}
                    </td>
                    <td className="px-4 py-3 text-right">
                      <button
                        onClick={() => startUpdate(tariff)}
                        className="text-xs text-teal-600 hover:text-teal-800"
                      >
                        Actualizar precio
                      </button>
                    </td>
                  </tr>
                  {showUpdateForm === tariff.tariffId && (
                    <tr key={`${tariff.tariffId}-update`}>
                      <td colSpan={5} className="px-4 py-3 bg-gray-50">
                        <form onSubmit={(e) => handleUpdate(tariff.tariffId, e)} className="flex items-end gap-4">
                          {formError && (
                            <span className="text-xs text-red-600">{formError}</span>
                          )}
                          <div>
                            <label className="block text-xs font-medium text-gray-600 mb-1">Nuevo Precio</label>
                            <input
                              required
                              type="number"
                              step="0.01"
                              min="0"
                              value={updateForm.basePrice}
                              onChange={(e) => setUpdateForm({ ...updateForm, basePrice: e.target.value })}
                              className="px-3 py-1.5 border border-gray-300 rounded-lg text-sm w-36"
                            />
                          </div>
                          <div>
                            <label className="block text-xs font-medium text-gray-600 mb-1">Vigente Desde</label>
                            <input
                              required
                              type="date"
                              value={updateForm.effectiveFrom}
                              onChange={(e) => setUpdateForm({ ...updateForm, effectiveFrom: e.target.value })}
                              className="px-3 py-1.5 border border-gray-300 rounded-lg text-sm"
                            />
                          </div>
                          <button
                            type="submit"
                            className="px-3 py-1.5 bg-teal-600 text-white text-sm rounded-lg hover:bg-teal-700"
                          >
                            Guardar
                          </button>
                          <button
                            type="button"
                            onClick={() => setShowUpdateForm(null)}
                            className="px-3 py-1.5 text-sm text-gray-500 hover:text-gray-700"
                          >
                            Cancelar
                          </button>
                        </form>
                      </td>
                    </tr>
                  )}
                </>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="mt-4 flex items-center justify-between">
          <p className="text-xs text-gray-500">
            Página {page + 1} de {totalPages}
          </p>
          <div className="flex gap-2">
            <button
              disabled={page === 0}
              onClick={() => setPage(page - 1)}
              className="px-3 py-1 text-sm border rounded-lg disabled:opacity-50 hover:bg-gray-50"
            >
              Anterior
            </button>
            <button
              disabled={page >= totalPages - 1}
              onClick={() => setPage(page + 1)}
              className="px-3 py-1 text-sm border rounded-lg disabled:opacity-50 hover:bg-gray-50"
            >
              Siguiente
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
