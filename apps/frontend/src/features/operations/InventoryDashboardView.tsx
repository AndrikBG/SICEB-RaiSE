import { useState, useEffect, useCallback } from 'react';
import { useAuthStore } from '@/stores/auth-store';
import {
  inventoryApi,
  type InventoryItemResponse,
  type InventoryQueryParams,
} from '@/lib/inventory-api';
import {
  subscribeInventory,
  unsubscribeInventory,
  type InventoryChangeEvent,
} from '@/lib/inventory-realtime';

const STATUS_ROW_COLORS: Record<string, string> = {
  OK: '',
  LOW_STOCK: 'bg-yellow-50',
  OUT_OF_STOCK: 'bg-red-50',
};

const EXPIRATION_ROW_COLORS: Record<string, string> = {
  OK: '',
  EXPIRING_SOON: 'bg-orange-50',
  EXPIRED: 'bg-red-100',
};

function rowColor(item: InventoryItemResponse): string {
  return EXPIRATION_ROW_COLORS[item.expirationStatus] || STATUS_ROW_COLORS[item.stockStatus] || '';
}

const STATUS_BADGE: Record<string, string> = {
  OK: 'bg-green-100 text-green-700',
  LOW_STOCK: 'bg-yellow-100 text-yellow-700',
  OUT_OF_STOCK: 'bg-red-100 text-red-700',
};

export function InventoryDashboardView() {
  const activeBranch = useAuthStore((s) => s.activeBranch);

  const [items, setItems] = useState<InventoryItemResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  const [filterStatus, setFilterStatus] = useState<string>('');
  const [filterCategory, setFilterCategory] = useState('');
  const [filterService, setFilterService] = useState('');
  const [search, setSearch] = useState('');

  const loadItems = useCallback(async (pageNum: number) => {
    setLoading(true);
    setError('');
    try {
      const params: InventoryQueryParams = { page: pageNum, size: 50 };
      if (filterStatus) params.filterStatus = filterStatus as InventoryQueryParams['filterStatus'];
      if (filterCategory) params.filterCategory = filterCategory;
      if (filterService) params.filterService = filterService;
      if (search) params.search = search;

      const res = await inventoryApi.list(params);
      setItems(res.data.content);
      setTotalPages(res.data.totalPages);
      setTotalElements(res.data.totalElements);
    } catch {
      setError('Error al cargar inventario');
    } finally {
      setLoading(false);
    }
  }, [filterStatus, filterCategory, filterService, search]);

  useEffect(() => { loadItems(page); }, [page, loadItems]);

  // Real-time WebSocket updates
  useEffect(() => {
    if (!activeBranch) return;
    subscribeInventory(activeBranch.id, (event: InventoryChangeEvent) => {
      setItems((prev) =>
        prev.map((item) =>
          item.itemId === event.itemId
            ? { ...item, currentStock: event.newStock, stockStatus: event.stockStatus }
            : item,
        ),
      );
    });
    return () => unsubscribeInventory();
  }, [activeBranch]);

  async function handleExport() {
    try {
      const res = await inventoryApi.exportExcel();
      const url = window.URL.createObjectURL(new Blob([res.data as BlobPart]));
      const a = document.createElement('a');
      a.href = url;
      a.download = 'inventario.xlsx';
      a.click();
      window.URL.revokeObjectURL(url);
    } catch {
      setError('Error al exportar');
    }
  }

  function handleSearch() {
    setPage(0);
    loadItems(0);
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-xl font-bold text-gray-900">Inventario General</h1>
          <p className="text-sm text-gray-500">{totalElements} artículos</p>
        </div>
        <button
          onClick={handleExport}
          className="px-4 py-2 bg-teal-600 text-white text-sm font-medium rounded-lg hover:bg-teal-700 transition-colors"
        >
          Exportar Excel
        </button>
      </div>

      {error && (
        <div className="mb-4 p-3 bg-red-50 text-red-700 text-sm rounded-lg">{error}</div>
      )}

      {/* Filters */}
      <div className="mb-4 flex gap-3 flex-wrap">
        <select
          value={filterStatus}
          onChange={(e) => { setFilterStatus(e.target.value); setPage(0); }}
          className="px-3 py-2 border border-gray-300 rounded-lg text-sm"
        >
          <option value="">Estado: Todos</option>
          <option value="OK">OK</option>
          <option value="LOW_STOCK">Stock Bajo</option>
          <option value="OUT_OF_STOCK">Sin Stock</option>
        </select>

        <input
          placeholder="Filtrar categoría..."
          value={filterCategory}
          onChange={(e) => { setFilterCategory(e.target.value); setPage(0); }}
          className="px-3 py-2 border border-gray-300 rounded-lg text-sm"
        />

        <input
          placeholder="Filtrar servicio (ID)..."
          value={filterService}
          onChange={(e) => { setFilterService(e.target.value); setPage(0); }}
          className="px-3 py-2 border border-gray-300 rounded-lg text-sm"
        />

        <div className="flex gap-1">
          <input
            placeholder="Buscar por nombre o SKU..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
            className="px-3 py-2 border border-gray-300 rounded-lg text-sm w-64"
          />
          <button
            onClick={handleSearch}
            className="px-3 py-2 bg-gray-100 hover:bg-gray-200 rounded-lg text-sm"
          >
            Buscar
          </button>
        </div>
      </div>

      {/* Table */}
      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 border-b border-gray-200">
            <tr>
              <th className="text-left px-4 py-3 font-medium text-gray-600">SKU</th>
              <th className="text-left px-4 py-3 font-medium text-gray-600">Nombre</th>
              <th className="text-left px-4 py-3 font-medium text-gray-600">Categoría</th>
              <th className="text-right px-4 py-3 font-medium text-gray-600">Stock</th>
              <th className="text-right px-4 py-3 font-medium text-gray-600">Mín</th>
              <th className="text-left px-4 py-3 font-medium text-gray-600">Unidad</th>
              <th className="text-left px-4 py-3 font-medium text-gray-600">Estado</th>
              <th className="text-left px-4 py-3 font-medium text-gray-600">Vencimiento</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {loading ? (
              <tr><td colSpan={8} className="px-4 py-8 text-center text-gray-400">Cargando...</td></tr>
            ) : items.length === 0 ? (
              <tr><td colSpan={8} className="px-4 py-8 text-center text-gray-400">Sin artículos</td></tr>
            ) : (
              items.map((item) => (
                <tr key={item.itemId} className={`hover:bg-gray-50 ${rowColor(item)}`}>
                  <td className="px-4 py-3 font-mono text-xs text-gray-600">{item.sku}</td>
                  <td className="px-4 py-3 font-medium text-gray-900">{item.name}</td>
                  <td className="px-4 py-3 text-gray-600">{item.category}</td>
                  <td className="px-4 py-3 text-right font-medium">{item.currentStock}</td>
                  <td className="px-4 py-3 text-right text-gray-500">{item.minThreshold}</td>
                  <td className="px-4 py-3 text-gray-500">{item.unitOfMeasure}</td>
                  <td className="px-4 py-3">
                    <span className={`inline-flex px-2 py-0.5 rounded-full text-xs font-medium ${STATUS_BADGE[item.stockStatus] ?? ''}`}>
                      {item.stockStatus.replace('_', ' ')}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-gray-500 text-xs">
                    {item.expirationDate
                      ? new Date(item.expirationDate).toLocaleDateString('es-MX')
                      : '—'}
                  </td>
                </tr>
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
