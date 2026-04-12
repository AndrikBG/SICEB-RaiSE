import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { clinicalApi, type TimelineEntry } from '@/lib/clinical-api';
import { useClinicalStore } from '@/stores/clinical-store';

type Tab = 'timeline' | 'nom004';

const EVENT_STYLES: Record<string, { badge: string; label: string }> = {
  RECORD_CREATED: { badge: 'bg-purple-100 text-purple-700', label: 'Registro' },
  CONSULTATION: { badge: 'bg-blue-100 text-blue-700', label: 'Consulta' },
  PRESCRIPTION: { badge: 'bg-green-100 text-green-700', label: 'Receta' },
  LAB_ORDER: { badge: 'bg-amber-100 text-amber-700', label: 'Lab - Orden' },
  LAB_RESULT: { badge: 'bg-teal-100 text-teal-700', label: 'Lab - Resultado' },
  ATTACHMENT: { badge: 'bg-gray-100 text-gray-700', label: 'Adjunto' },
};

export function MedicalRecordView() {
  const { patientId } = useParams<{ patientId: string }>();
  const { activePatient, timeline, setTimeline } = useClinicalStore();
  const [tab, setTab] = useState<Tab>('timeline');
  const [nom004, setNom004] = useState<Record<string, unknown> | null>(null);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  useEffect(() => {
    if (!patientId) return;
    setLoading(true);
    clinicalApi.getTimeline(patientId, page, 20)
      .then((res) => {
        setTimeline(res.data.content ?? []);
        setTotalPages(res.data.totalPages ?? 0);
      })
      .catch(() => setTimeline([]))
      .finally(() => setLoading(false));
  }, [patientId, page, setTimeline]);

  useEffect(() => {
    if (tab === 'nom004' && patientId && !nom004) {
      clinicalApi.getNom004(patientId)
        .then((res) => setNom004(res.data))
        .catch(() => setNom004(null));
    }
  }, [tab, patientId, nom004]);

  const formatDateTime = (iso: string) => {
    try {
      return new Date(iso).toLocaleString('es-MX', { dateStyle: 'medium', timeStyle: 'short' });
    } catch { return iso; }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <Link to="/patients" className="text-sm text-purple-600 hover:text-purple-800">&larr; Volver a búsqueda</Link>
          <h1 className="text-2xl font-bold text-gray-900 mt-1">
            {activePatient?.fullName ?? 'Expediente Médico'}
          </h1>
          {activePatient && (
            <p className="text-sm text-gray-500">
              {activePatient.patientType} &middot; {activePatient.dateOfBirth}
            </p>
          )}
        </div>
        <Link
          to={`/patients/${patientId}/consultation`}
          className="bg-purple-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-purple-700 transition-colors"
        >
          + Nueva Consulta
        </Link>
      </div>

      <div className="flex gap-1 border-b border-gray-200">
        <TabButton active={tab === 'timeline'} onClick={() => setTab('timeline')}>Timeline Clínico</TabButton>
        <TabButton active={tab === 'nom004'} onClick={() => setTab('nom004')}>NOM-004</TabButton>
      </div>

      {tab === 'timeline' && (
        <div className="space-y-3">
          {loading ? (
            <div className="text-center py-12 text-gray-400">Cargando timeline...</div>
          ) : timeline.length === 0 ? (
            <div className="text-center py-12 text-gray-400">Sin eventos clínicos registrados</div>
          ) : (
            <>
              {timeline.map((entry) => (
                <TimelineCard key={entry.eventId} entry={entry} formatDateTime={formatDateTime} />
              ))}
              {totalPages > 1 && (
                <div className="flex justify-center gap-2 pt-4">
                  <button disabled={page === 0} onClick={() => setPage((p) => p - 1)}
                    className="px-3 py-1 text-sm border rounded disabled:opacity-30">Anterior</button>
                  <span className="px-3 py-1 text-sm text-gray-500">{page + 1} / {totalPages}</span>
                  <button disabled={page >= totalPages - 1} onClick={() => setPage((p) => p + 1)}
                    className="px-3 py-1 text-sm border rounded disabled:opacity-30">Siguiente</button>
                </div>
              )}
            </>
          )}
        </div>
      )}

      {tab === 'nom004' && nom004 && (
        <Nom004View data={nom004} />
      )}
    </div>
  );
}

function TabButton({ active, onClick, children }: { active: boolean; onClick: () => void; children: React.ReactNode }) {
  return (
    <button
      onClick={onClick}
      className={`px-4 py-2 text-sm font-medium border-b-2 transition-colors -mb-px ${
        active ? 'border-purple-600 text-purple-700' : 'border-transparent text-gray-500 hover:text-gray-700'
      }`}
    >
      {children}
    </button>
  );
}

const PAYLOAD_LABELS: Record<string, string> = {
  subjective: 'Subjetivo (síntomas)',
  objective: 'Objetivo (exploración)',
  diagnosis: 'Diagnóstico',
  diagnosisCode: 'Código CIE-10',
  plan: 'Plan de tratamiento',
  vitalSigns: 'Signos vitales',
  requiresSupervision: 'Requiere supervisión',
  supervisorStaffId: 'Supervisor',
  medicationName: 'Medicamento',
  dosage: 'Dosis',
  frequency: 'Frecuencia',
  duration: 'Duración',
  route: 'Vía de administración',
  instructions: 'Instrucciones',
  quantity: 'Cantidad',
  studyType: 'Tipo de estudio',
  priority: 'Prioridad',
  status: 'Estado',
  resultText: 'Resultado',
  items: 'Medicamentos',
  studies: 'Estudios',
  recordId: 'Expediente',
  consultationId: 'ID Consulta',
  prescriptionId: 'ID Receta',
  patientType: 'Tipo de paciente',
  firstName: 'Nombre',
  paternalSurname: 'Apellido paterno',
  maternalSurname: 'Apellido materno',
  dateOfBirth: 'Fecha de nacimiento',
  gender: 'Género',
  phone: 'Teléfono',
  curp: 'CURP',
  credentialNumber: 'No. credencial',
  fullName: 'Nombre completo',
  email: 'Correo electrónico',
};

const PRIORITY_LABELS: Record<string, string> = { ROUTINE: 'Rutina', URGENT: 'Urgente' };
const GENDER_LABELS: Record<string, string> = { MALE: 'Masculino', FEMALE: 'Femenino', OTHER: 'Otro' };

function formatValue(key: string, value: unknown): string {
  if (value === null || value === undefined) return '—';
  if (typeof value === 'boolean') return value ? 'Sí' : 'No';
  if (key === 'priority' && typeof value === 'string') return PRIORITY_LABELS[value] ?? value;
  if (key === 'gender' && typeof value === 'string') return GENDER_LABELS[value] ?? value;
  if (typeof value === 'string' && /^\d{4}-\d{2}-\d{2}/.test(value)) {
    try {
      return new Date(value).toLocaleDateString('es-MX', { dateStyle: 'medium' });
    } catch { return value; }
  }
  if (typeof value === 'object') return JSON.stringify(value);
  return String(value);
}

function ReadableData({ data, depth = 0 }: Readonly<{ data: unknown; depth?: number }>) {
  if (data === null || data === undefined) return <span className="text-gray-400">Sin datos</span>;
  if (typeof data === 'string') return <span className="text-gray-700">{data}</span>;
  if (typeof data === 'number' || typeof data === 'boolean') return <span className="text-gray-700">{formatValue('', data)}</span>;

  if (Array.isArray(data)) {
    if (data.length === 0) return <span className="text-gray-400">Sin elementos</span>;
    return (
      <div className="space-y-2">
        {data.map((item, i) => (
          <div key={i} className={`${depth === 0 ? 'bg-gray-50 rounded-lg p-3' : 'border-l-2 border-gray-200 pl-3 py-1'}`}>
            {typeof item === 'object' && item !== null ? (
              <ReadableData data={item} depth={depth + 1} />
            ) : (
              <span className="text-sm text-gray-700">{formatValue('', item)}</span>
            )}
          </div>
        ))}
      </div>
    );
  }

  if (typeof data === 'object') {
    const entries = Object.entries(data as Record<string, unknown>).filter(
      ([k, v]) => v !== null && v !== undefined && !k.endsWith('Key') && k !== 'idempotencyKey'
    );
    if (entries.length === 0) return <span className="text-gray-400">Sin datos</span>;

    return (
      <dl className={`grid gap-y-2 ${depth === 0 ? 'gap-x-4' : 'gap-x-3'}`}>
        {entries.map(([key, value]) => {
          const label = PAYLOAD_LABELS[key] ?? key.replaceAll(/([A-Z])/g, ' $1').replace(/^./, (c) => c.toUpperCase());

          if (Array.isArray(value) || (typeof value === 'object' && value !== null)) {
            return (
              <div key={key} className="col-span-full">
                <dt className="text-xs font-medium text-gray-500 mb-1">{label}</dt>
                <dd><ReadableData data={value} depth={depth + 1} /></dd>
              </div>
            );
          }

          return (
            <div key={key} className="flex gap-2 items-baseline">
              <dt className="text-xs font-medium text-gray-500 whitespace-nowrap">{label}:</dt>
              <dd className="text-sm text-gray-800">{formatValue(key, value)}</dd>
            </div>
          );
        })}
      </dl>
    );
  }

  return <span className="text-gray-700">{typeof data === 'object' ? JSON.stringify(data) : String(data)}</span>;
}

function TimelineCard({ entry, formatDateTime }: Readonly<{ entry: TimelineEntry; formatDateTime: (iso: string) => string }>) {
  const [expanded, setExpanded] = useState(false);
  const style = EVENT_STYLES[entry.eventType] ?? { badge: 'bg-gray-100 text-gray-700', label: entry.eventType };

  return (
    <div className="bg-white rounded-xl border border-gray-200 p-4 hover:shadow-sm transition-shadow">
      <button type="button" className="flex items-center justify-between cursor-pointer w-full text-left" onClick={() => setExpanded(!expanded)}>
        <div className="flex items-center gap-3">
          <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${style.badge}`}>{style.label}</span>
          <span className="text-sm text-gray-900">{entry.summary}</span>
        </div>
        <div className="flex items-center gap-2">
          <span className="text-xs text-gray-400">{formatDateTime(entry.occurredAt)}</span>
          <svg className={`w-4 h-4 text-gray-400 transition-transform ${expanded ? 'rotate-180' : ''}`} fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
          </svg>
        </div>
      </button>
      {expanded && (
        <div className="mt-3 p-3 bg-gray-50 rounded-lg text-sm">
          <ReadableData data={entry.payload} />
        </div>
      )}
    </div>
  );
}

function Nom004View({ data }: Readonly<{ data: Record<string, unknown> }>) {
  const sections = [
    { key: 'identification', title: 'Identificación del Paciente' },
    { key: 'clinicalNotes', title: 'Notas Clínicas' },
    { key: 'diagnostics', title: 'Diagnósticos' },
    { key: 'labSummaries', title: 'Laboratorio' },
    { key: 'prescriptions', title: 'Recetas' },
    { key: 'attachments', title: 'Adjuntos' },
  ];

  return (
    <div className="space-y-4">
      <p className="text-xs text-gray-500 uppercase tracking-wide">Expediente conforme a NOM-004-SSA3-2012</p>
      {sections.map(({ key, title }) => (
        <div key={key} className="bg-white rounded-xl border border-gray-200 p-4">
          <h3 className="text-sm font-semibold text-gray-800 mb-3">{title}</h3>
          <ReadableData data={data[key]} />
        </div>
      ))}
    </div>
  );
}
