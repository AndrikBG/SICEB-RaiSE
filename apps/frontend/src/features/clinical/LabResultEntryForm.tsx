import { useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { clinicalApi } from '@/lib/clinical-api';

export function LabResultEntryForm() {
  const { studyId } = useParams<{ studyId: string }>();
  const navigate = useNavigate();
  const [resultText, setResultText] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!studyId || !resultText.trim()) return;
    setSubmitting(true);
    setError('');
    try {
      await clinicalApi.recordLabResult(studyId, {
        studyId,
        resultId: crypto.randomUUID(),
        resultText: resultText.trim(),
        idempotencyKey: crypto.randomUUID(),
      });
      navigate('/lab');
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message ?? 'Error al registrar resultado';
      setError(msg);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      <Link to="/lab" className="text-sm text-purple-600 hover:text-purple-800">&larr; Volver a estudios pendientes</Link>
      <h1 className="text-2xl font-bold text-gray-900">Registrar Resultado de Laboratorio</h1>
      <p className="text-sm text-gray-500">Estudio: {studyId?.slice(0, 8)}...</p>

      {error && <div className="p-3 bg-red-50 text-red-700 rounded-lg text-sm">{error}</div>}

      <form onSubmit={handleSubmit} className="bg-white rounded-xl border border-gray-200 p-6 space-y-4">
        <label className="block">
          <span className="text-sm font-medium text-gray-700">Resultado (texto)</span>
          <textarea
            value={resultText}
            onChange={(e) => setResultText(e.target.value)}
            required
            rows={10}
            placeholder="Ingrese los resultados del estudio..."
            className="mt-1 block w-full border border-gray-300 rounded-lg px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent resize-none"
          />
        </label>

        <button
          type="submit"
          disabled={submitting || !resultText.trim()}
          className="w-full bg-green-600 text-white py-2.5 rounded-lg font-medium hover:bg-green-700 disabled:opacity-50 transition-colors"
        >
          {submitting ? 'Guardando...' : 'Validar y Publicar Resultado'}
        </button>
      </form>
    </div>
  );
}
