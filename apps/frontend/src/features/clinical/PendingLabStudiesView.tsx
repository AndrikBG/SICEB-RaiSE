import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { clinicalApi } from '@/lib/clinical-api';
import { useClinicalStore } from '@/stores/clinical-store';

export function PendingLabStudiesView() {
  const navigate = useNavigate();
  const { pendingLabStudies, setPendingLabStudies, setLoading, isLoading } = useClinicalStore();
  const [statusFilter, setStatusFilter] = useState<string>('');

  useEffect(() => {
    loadStudies();
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [statusFilter]);

  const loadStudies = async () => {
    setLoading(true);
    try {
      const res = await clinicalApi.getPendingLabStudies(statusFilter || undefined);
      setPendingLabStudies(Array.isArray(res.data) ? res.data : []);
    } catch {
      setPendingLabStudies([]);
    } finally {
      setLoading(false);
    }
  };

  const formatDateTime = (iso: string) => {
    try { return new Date(iso).toLocaleString('es-MX', { dateStyle: 'short', timeStyle: 'short' }); } catch { return iso; }
  };

  const statusStyles: Record<string, string> = {
    PENDING: 'bg-amber-100 text-amber-700',
    IN_PROGRESS: 'bg-blue-100 text-blue-700',
    COMPLETED: 'bg-green-100 text-green-700',
    REJECTED: 'bg-red-100 text-red-700',
  };

  const priorityStyles: Record<string, string> = {
    ROUTINE: '',
    URGENT: 'bg-red-50 border-red-200',
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">Laboratorio — Estudios Pendientes</h1>
        <button onClick={loadStudies} disabled={isLoading}
          className="text-sm text-purple-600 hover:text-purple-800 font-medium">
          Actualizar
        </button>
      </div>

      <div className="flex gap-2">
        {['', 'PENDING', 'IN_PROGRESS', 'COMPLETED'].map((s) => (
          <button
            key={s}
            onClick={() => setStatusFilter(s)}
            className={`px-3 py-1.5 text-xs font-medium rounded-full border transition-colors ${
              statusFilter === s ? 'bg-purple-100 text-purple-700 border-purple-300' : 'bg-white text-gray-600 border-gray-200 hover:bg-gray-50'
            }`}
          >
            {s === '' ? 'Todos pendientes' : s === 'PENDING' ? 'Pendiente' : s === 'IN_PROGRESS' ? 'En proceso' : 'Completado'}
          </button>
        ))}
      </div>

      {isLoading ? (
        <div className="text-center py-12 text-gray-400">Cargando estudios...</div>
      ) : pendingLabStudies.length === 0 ? (
        <div className="text-center py-12 text-gray-400">No hay estudios pendientes</div>
      ) : (
        <div className="space-y-3">
          {pendingLabStudies.map((study) => (
            <div
              key={study.studyId}
              className={`bg-white rounded-xl border border-gray-200 p-4 hover:shadow-sm transition-shadow cursor-pointer ${priorityStyles[study.priority] ?? ''}`}
              onClick={() => study.status === 'PENDING' || study.status === 'IN_PROGRESS'
                ? navigate(`/lab/${study.studyId}/result`)
                : undefined}
            >
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  {study.priority === 'URGENT' && (
                    <span className="text-red-600 text-xs font-bold animate-pulse">URGENTE</span>
                  )}
                  <span className="font-medium text-gray-900">{study.studyType}</span>
                  <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${statusStyles[study.status] ?? 'bg-gray-100 text-gray-600'}`}>
                    {study.status}
                  </span>
                </div>
                <span className="text-xs text-gray-400">{formatDateTime(study.requestedAt)}</span>
              </div>
              <div className="mt-2 flex items-center gap-4 text-sm text-gray-600">
                <span>Paciente: {study.patientName || study.patientId.slice(0, 8)}</span>
                {study.instructions && <span className="text-xs text-gray-400">| {study.instructions}</span>}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
