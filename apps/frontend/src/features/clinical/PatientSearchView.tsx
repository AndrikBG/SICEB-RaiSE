import { useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { clinicalApi, type PatientSearchResult } from '@/lib/clinical-api';
import { useClinicalStore } from '@/stores/clinical-store';

export function PatientSearchView() {
  const navigate = useNavigate();
  const { searchQuery, searchResults, setSearchQuery, setSearchResults, setActivePatient, setLoading, isLoading } = useClinicalStore();
  const [showNewForm, setShowNewForm] = useState(false);

  const handleSearch = useCallback(async () => {
    if (!searchQuery.trim()) return;
    setLoading(true);
    try {
      const res = await clinicalApi.searchPatients({ q: searchQuery });
      setSearchResults(res.data.content ?? []);
    } catch {
      setSearchResults([]);
    } finally {
      setLoading(false);
    }
  }, [searchQuery, setLoading, setSearchResults]);

  const selectPatient = (patient: PatientSearchResult) => {
    setActivePatient(patient);
    navigate(`/patients/${patient.patientId}`);
  };

  const formatDate = (iso: string) => {
    try { return new Date(iso).toLocaleDateString('es-MX'); } catch { return iso; }
  };

  const typeLabel: Record<string, string> = {
    STUDENT: 'Estudiante',
    WORKER: 'Trabajador',
    EXTERNAL: 'Externo',
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">Pacientes</h1>
        <button
          onClick={() => setShowNewForm(true)}
          className="bg-purple-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-purple-700 transition-colors"
        >
          + Nuevo Paciente
        </button>
      </div>

      <div className="flex gap-3">
        <input
          type="text"
          placeholder="Buscar por nombre, CURP, credencial, teléfono o ID..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
          className="flex-1 border border-gray-300 rounded-lg px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent"
        />
        <button
          onClick={handleSearch}
          disabled={isLoading}
          className="bg-gray-900 text-white px-6 py-2 rounded-lg text-sm font-medium hover:bg-gray-800 disabled:opacity-50 transition-colors"
        >
          {isLoading ? 'Buscando...' : 'Buscar'}
        </button>
      </div>

      {searchResults.length > 0 && (
        <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
          <table className="w-full text-sm">
            <thead className="bg-gray-50 text-gray-600 text-left">
              <tr>
                <th className="px-4 py-3 font-medium">Nombre</th>
                <th className="px-4 py-3 font-medium">Nacimiento</th>
                <th className="px-4 py-3 font-medium">Tipo</th>
                <th className="px-4 py-3 font-medium">Última visita</th>
                <th className="px-4 py-3 font-medium">Consultas</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {searchResults.map((patient) => (
                <tr
                  key={patient.patientId}
                  onClick={() => selectPatient(patient)}
                  className="hover:bg-purple-50 cursor-pointer transition-colors"
                >
                  <td className="px-4 py-3 font-medium text-gray-900">{patient.fullName}</td>
                  <td className="px-4 py-3 text-gray-600">{formatDate(patient.dateOfBirth)}</td>
                  <td className="px-4 py-3">
                    <span className={`inline-flex px-2 py-0.5 rounded-full text-xs font-medium ${
                      patient.patientType === 'STUDENT' ? 'bg-blue-100 text-blue-700' :
                      patient.patientType === 'WORKER' ? 'bg-green-100 text-green-700' :
                      'bg-gray-100 text-gray-700'
                    }`}>
                      {typeLabel[patient.patientType] ?? patient.patientType}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-gray-600">
                    {patient.lastVisitDate ? formatDate(patient.lastVisitDate) : '—'}
                  </td>
                  <td className="px-4 py-3 text-gray-600">{patient.consultationCount}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {searchResults.length === 0 && searchQuery && !isLoading && (
        <div className="text-center py-12 text-gray-500">
          <p className="text-lg">No se encontraron pacientes</p>
          <p className="text-sm mt-1">Intenta con otro término o registra un nuevo paciente</p>
        </div>
      )}

      {showNewForm && <NewPatientModal onClose={() => setShowNewForm(false)} />}
    </div>
  );
}

function NewPatientModal({ onClose }: { onClose: () => void }) {
  const { setActivePatient } = useClinicalStore();
  type NewPatientForm = {
    firstName: string;
    paternalSurname: string;
    maternalSurname: string;
    dateOfBirth: string;
    gender: 'MALE' | 'FEMALE' | 'OTHER';
    phone: string;
    curp: string;
    patientType: 'STUDENT' | 'WORKER' | 'EXTERNAL';
    credentialNumber: string;
    guardianName: string;
    guardianRelationship: string;
    guardianPhone: string;
    guardianIdConfirmed: boolean;
    dataConsentGiven: boolean;
    specialCase: boolean;
    specialCaseNotes: string;
  };

  const [form, setForm] = useState<NewPatientForm>({
    firstName: '', paternalSurname: '', maternalSurname: '', dateOfBirth: '',
    gender: 'MALE', phone: '', curp: '', patientType: 'EXTERNAL',
    credentialNumber: '', guardianName: '', guardianRelationship: '', guardianPhone: '',
    guardianIdConfirmed: false, dataConsentGiven: false, specialCase: false, specialCaseNotes: '',
  });
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    setError('');
    try {
      const duplicates = await clinicalApi.findPotentialDuplicates({
        firstName: form.firstName,
        paternalSurname: form.paternalSurname,
        maternalSurname: form.maternalSurname || undefined,
        dateOfBirth: form.dateOfBirth,
      });
      if (duplicates.data.length > 0) {
        const duplicateNames = duplicates.data.map((p) => p.fullName).join(', ');
        const proceed = window.confirm(
          `Posible paciente duplicado: ${duplicateNames}. ¿Deseas continuar con el alta de todas formas?`
        );
        if (!proceed) {
          setSubmitting(false);
          return;
        }
      }

      const res = await clinicalApi.createPatient({
        ...form,
        patientId: crypto.randomUUID(),
        idempotencyKey: crypto.randomUUID(),
      });
      setActivePatient({
        patientId: res.data.patientId,
        fullName: `${form.firstName} ${form.paternalSurname}${form.maternalSurname ? ` ${form.maternalSurname}` : ''}`.trim(),
        dateOfBirth: form.dateOfBirth,
        patientType: form.patientType,
        gender: form.gender,
        phone: form.phone || null,
        curp: form.curp || null,
        credentialNumber: form.credentialNumber || null,
        profileStatus: form.curp ? 'COMPLETE' : 'INCOMPLETE',
        branchId: '00000000-0000-4000-a000-000000000001',
        recordId: res.data.recordId,
        lastVisitDate: null,
        consultationCount: 0,
      });
      onClose();
      navigate(`/patients/${res.data.patientId}`);
    } catch (err: unknown) {
      setError(extractApiError(err, 'Error al registrar paciente'));
    } finally {
      setSubmitting(false);
    }
  };

  const update = (field: string, value: string | boolean) =>
    setForm((prev) => ({ ...prev, [field]: value }));

  const calculateAge = (dob: string) => {
    if (!dob) return null;
    const birth = new Date(dob);
    if (Number.isNaN(birth.getTime())) return null;
    const today = new Date();
    let age = today.getFullYear() - birth.getFullYear();
    const m = today.getMonth() - birth.getMonth();
    if (m < 0 || (m === 0 && today.getDate() < birth.getDate())) age--;
    return age;
  };

  const age = calculateAge(form.dateOfBirth);
  const isMinor = age !== null && age < 17;
  const needsCredential = form.patientType === 'STUDENT' || form.patientType === 'WORKER';

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-2xl max-w-lg w-full max-h-[90vh] overflow-y-auto p-6">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-xl font-bold text-gray-900">Nuevo Paciente</h2>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600 text-xl">&times;</button>
        </div>

        {error && <div className="mb-4 p-3 bg-red-50 text-red-700 rounded-lg text-sm">{error}</div>}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-2 gap-3">
            <Input label="Nombre(s) *" value={form.firstName} onChange={(v) => update('firstName', v)} required />
            <Input label="Apellido paterno *" value={form.paternalSurname} onChange={(v) => update('paternalSurname', v)} required />
            <Input label="Apellido materno" value={form.maternalSurname} onChange={(v) => update('maternalSurname', v)} />
            <Input label="Fecha de nacimiento *" value={form.dateOfBirth} onChange={(v) => update('dateOfBirth', v)} type="date" required />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <Select label="Género *" value={form.gender} onChange={(v) => update('gender', v)}
              options={[['MALE', 'Masculino'], ['FEMALE', 'Femenino'], ['OTHER', 'Otro']]} />
            <Select label="Tipo *" value={form.patientType} onChange={(v) => update('patientType', v)}
              options={[['EXTERNAL', 'Externo (0%)'], ['STUDENT', 'Estudiante (30%)'], ['WORKER', 'Trabajador (20%)']]} />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <Input label="Teléfono" value={form.phone} onChange={(v) => update('phone', v)} />
            <Input label="CURP" value={form.curp} onChange={(v) => update('curp', v)} />
          </div>
          {needsCredential && (
            <Input
              label="Número de credencial *"
              value={form.credentialNumber}
              onChange={(v) => update('credentialNumber', v)}
              required
            />
          )}
          {isMinor && (
            <>
              <div className="grid grid-cols-2 gap-3">
                <Input label="Nombre del tutor *" value={form.guardianName} onChange={(v) => update('guardianName', v)} required={!form.specialCase} />
                <Input label="Parentesco del tutor *" value={form.guardianRelationship} onChange={(v) => update('guardianRelationship', v)} required={!form.specialCase} />
              </div>
              <Input label="Teléfono del tutor" value={form.guardianPhone} onChange={(v) => update('guardianPhone', v)} />
              <label className="flex items-center gap-2 text-sm text-gray-700">
                <input
                  type="checkbox"
                  checked={form.guardianIdConfirmed}
                  onChange={(e) => update('guardianIdConfirmed', e.target.checked)}
                  className="rounded border-gray-300 text-purple-600 focus:ring-purple-500"
                />
                Identidad oficial del tutor confirmada
              </label>
              <label className="flex items-center gap-2 text-sm text-gray-700">
                <input
                  type="checkbox"
                  checked={form.specialCase}
                  onChange={(e) => update('specialCase', e.target.checked)}
                  className="rounded border-gray-300 text-purple-600 focus:ring-purple-500"
                />
                Caso especial (omite validación de tutor para menor)
              </label>
              {form.specialCase && (
                <Input
                  label="Notas de caso especial"
                  value={form.specialCaseNotes}
                  onChange={(v) => update('specialCaseNotes', v)}
                />
              )}
            </>
          )}
          <label className="flex items-center gap-2 text-sm text-gray-700">
            <input type="checkbox" checked={form.dataConsentGiven} onChange={(e) => update('dataConsentGiven', e.target.checked)}
              className="rounded border-gray-300 text-purple-600 focus:ring-purple-500" />
            Consentimiento de datos otorgado
          </label>
          <button
            type="submit"
            disabled={submitting}
            className="w-full bg-purple-600 text-white py-2.5 rounded-lg font-medium hover:bg-purple-700 disabled:opacity-50 transition-colors"
          >
            {submitting ? 'Registrando...' : 'Registrar Paciente'}
          </button>
        </form>
      </div>
    </div>
  );
}

function extractApiError(err: unknown, fallback: string) {
  const data = (err as { response?: { data?: { message?: string; details?: Record<string, string> } } })?.response?.data;
  if (!data) return fallback;
  const detailText = data.details ? ` ${Object.entries(data.details).map(([k, v]) => `${k}: ${v}`).join(' | ')}` : '';
  return `${data.message ?? fallback}${detailText}`;
}

function Input({ label, value, onChange, type = 'text', required }: {
  label: string; value: string; onChange: (v: string) => void; type?: string; required?: boolean;
}) {
  return (
    <label className="block">
      <span className="text-xs font-medium text-gray-600">{label}</span>
      <input type={type} value={value} onChange={(e) => onChange(e.target.value)} required={required}
        className="mt-1 block w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent" />
    </label>
  );
}

function Select({ label, value, onChange, options }: {
  label: string; value: string; onChange: (v: string) => void; options: string[][];
}) {
  return (
    <label className="block">
      <span className="text-xs font-medium text-gray-600">{label}</span>
      <select value={value} onChange={(e) => onChange(e.target.value)}
        className="mt-1 block w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent">
        {options.map(([val, lbl]) => <option key={val} value={val}>{lbl}</option>)}
      </select>
    </label>
  );
}
