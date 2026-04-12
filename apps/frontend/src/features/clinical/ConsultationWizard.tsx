import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { clinicalApi } from '@/lib/clinical-api';
import { useClinicalStore } from '@/stores/clinical-store';

const STEPS = ['Signos Vitales y Diagnóstico', 'Recetas', 'Laboratorio', 'Revisión y Confirmación'];

export function ConsultationWizard() {
  const { patientId } = useParams<{ patientId: string }>();
  const navigate = useNavigate();
  const store = useClinicalStore();
  const { wizard } = store;
  const [submitError, setSubmitError] = useState('');

  useEffect(() => {
    if (store.activePatient?.recordId) {
      store.updateWizard({
        recordId: store.activePatient.recordId,
        consultationId: crypto.randomUUID(),
      });
    }
    return () => store.resetWizard();
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const canAdvance = () => {
    if (wizard.step === 0) return wizard.subjective.trim() && wizard.diagnosis.trim() && wizard.plan.trim();
    return true;
  };

  const handleSubmit = async () => {
    if (!wizard.recordId || !wizard.consultationId) return;
    store.setLoading(true);
    setSubmitError('');
    try {
      const idemBase = crypto.randomUUID();

      await clinicalApi.addConsultation({
        consultationId: wizard.consultationId,
        recordId: wizard.recordId,
        subjective: wizard.subjective,
        objective: wizard.objective,
        diagnosis: wizard.diagnosis,
        diagnosisCode: wizard.diagnosisCode || undefined,
        plan: wizard.plan,
        vitalSigns: wizard.vitalSigns || undefined,
        requiresSupervision: wizard.requiresSupervision,
        idempotencyKey: idemBase + '-CONSULT',
      });

      if (wizard.prescriptionItems.length > 0) {
        await clinicalApi.createPrescription(wizard.consultationId, {
          prescriptionId: crypto.randomUUID(),
          consultationId: wizard.consultationId,
          recordId: wizard.recordId,
          items: wizard.prescriptionItems.map((item) => ({
            ...item,
            medicationId: item.medicationId || crypto.randomUUID(),
          })),
          idempotencyKey: idemBase + '-RX',
        });
      }

      if (wizard.labStudies.length > 0) {
        await clinicalApi.createLabStudies(wizard.consultationId, {
          consultationId: wizard.consultationId,
          recordId: wizard.recordId,
          studies: wizard.labStudies.map((s) => ({
            ...s,
            studyId: s.studyId || crypto.randomUUID(),
          })),
          idempotencyKey: idemBase + '-LAB',
        });
      }

      navigate(`/patients/${patientId}`);
    } catch (err: unknown) {
      const axiosErr = err as { response?: { status?: number } };
      if (axiosErr.response?.status === 403) {
        setSubmitError('No tienes permisos para registrar consultas. Tu rol actual no incluye el permiso "consultation:create". Contacta al administrador para que asigne los permisos necesarios.');
      } else if (axiosErr.response?.status === 401) {
        setSubmitError('Tu sesión ha expirado. Por favor inicia sesión de nuevo.');
      } else {
        setSubmitError('Ocurrió un error al guardar la consulta. Intenta de nuevo.');
      }
    } finally {
      store.setLoading(false);
    }
  };

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">Nueva Consulta</h1>
      <p className="text-sm text-gray-500">{store.activePatient?.fullName ?? patientId}</p>

      <StepIndicator current={wizard.step} steps={STEPS} />

      {submitError && (
        <div className="bg-red-50 border border-red-200 rounded-xl px-4 py-3 text-sm text-red-700">
          {submitError}
        </div>
      )}

      <div className="bg-white rounded-xl border border-gray-200 p-6">
        {wizard.step === 0 && <Step1VitalsDiagnosis />}
        {wizard.step === 1 && <Step2Prescriptions />}
        {wizard.step === 2 && <Step3LabOrders />}
        {wizard.step === 3 && <Step4Review />}
      </div>

      <div className="flex justify-between">
        <button
          onClick={() => wizard.step > 0 ? store.setWizardStep(wizard.step - 1) : navigate(-1)}
          className="px-4 py-2 text-sm font-medium text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
        >
          {wizard.step === 0 ? 'Cancelar' : 'Anterior'}
        </button>
        {wizard.step < 3 ? (
          <button
            onClick={() => store.setWizardStep(wizard.step + 1)}
            disabled={!canAdvance()}
            className="px-6 py-2 text-sm font-medium text-white bg-purple-600 rounded-lg hover:bg-purple-700 disabled:opacity-50 transition-colors"
          >
            Siguiente
          </button>
        ) : (
          <button
            onClick={handleSubmit}
            disabled={store.isLoading}
            className="px-6 py-2 text-sm font-medium text-white bg-green-600 rounded-lg hover:bg-green-700 disabled:opacity-50 transition-colors"
          >
            {store.isLoading ? 'Guardando...' : 'Confirmar Consulta'}
          </button>
        )}
      </div>
    </div>
  );
}

function StepIndicator({ current, steps }: { current: number; steps: string[] }) {
  return (
    <div className="flex items-center gap-2">
      {steps.map((label, i) => (
        <div key={i} className="flex items-center gap-2">
          <div className={`w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold ${
            i <= current ? 'bg-purple-600 text-white' : 'bg-gray-200 text-gray-500'
          }`}>{i + 1}</div>
          <span className={`text-xs hidden sm:inline ${i <= current ? 'text-purple-700 font-medium' : 'text-gray-400'}`}>
            {label}
          </span>
          {i < steps.length - 1 && <div className={`w-8 h-0.5 ${i < current ? 'bg-purple-600' : 'bg-gray-200'}`} />}
        </div>
      ))}
    </div>
  );
}

function Step1VitalsDiagnosis() {
  const { wizard, updateWizard } = useClinicalStore();
  return (
    <div className="space-y-4">
      <h3 className="font-semibold text-gray-800">Paso 1 — Signos Vitales y Diagnóstico</h3>
      <Textarea label="Signos vitales" value={wizard.vitalSigns} onChange={(v) => updateWizard({ vitalSigns: v })} placeholder="TA: 120/80, FC: 72, Temp: 36.5°C, Peso: 70kg" />
      <Textarea label="Subjetivo (síntomas) *" value={wizard.subjective} onChange={(v) => updateWizard({ subjective: v })} required />
      <Textarea label="Objetivo (exploración) *" value={wizard.objective} onChange={(v) => updateWizard({ objective: v })} required />
      <div className="grid grid-cols-2 gap-3">
        <TextInput label="Diagnóstico *" value={wizard.diagnosis} onChange={(v) => updateWizard({ diagnosis: v })} required />
        <TextInput label="Código CIE-10" value={wizard.diagnosisCode} onChange={(v) => updateWizard({ diagnosisCode: v })} placeholder="J00" />
      </div>
      <Textarea label="Plan de tratamiento *" value={wizard.plan} onChange={(v) => updateWizard({ plan: v })} required />
      <label className="flex items-center gap-2 text-sm">
        <input type="checkbox" checked={wizard.requiresSupervision}
          onChange={(e) => updateWizard({ requiresSupervision: e.target.checked })}
          className="rounded border-gray-300 text-purple-600 focus:ring-purple-500" />
        Requiere supervisión (R1/R2)
      </label>
    </div>
  );
}

function Step2Prescriptions() {
  const { wizard, addPrescriptionItem, removePrescriptionItem, updatePrescriptionItem } = useClinicalStore();
  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h3 className="font-semibold text-gray-800">Paso 2 — Recetas</h3>
        <button onClick={addPrescriptionItem} className="text-sm text-purple-600 hover:text-purple-800 font-medium">+ Agregar medicamento</button>
      </div>
      {wizard.prescriptionItems.length === 0 && (
        <p className="text-sm text-gray-400 italic">Sin medicamentos (opcional)</p>
      )}
      {wizard.prescriptionItems.map((item, i) => (
        <div key={i} className="border border-gray-200 rounded-lg p-3 space-y-2">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-gray-500">Medicamento {i + 1}</span>
            <button onClick={() => removePrescriptionItem(i)} className="text-red-500 text-xs hover:text-red-700">Eliminar</button>
          </div>
          <div className="grid grid-cols-2 gap-2">
            <TextInput label="Nombre" value={item.medicationName} onChange={(v) => updatePrescriptionItem(i, 'medicationName', v)} />
            <TextInput label="Dosis" value={item.dosage} onChange={(v) => updatePrescriptionItem(i, 'dosage', v)} placeholder="500mg" />
            <TextInput label="Frecuencia" value={item.frequency} onChange={(v) => updatePrescriptionItem(i, 'frequency', v)} placeholder="c/8hrs" />
            <TextInput label="Duración" value={item.duration} onChange={(v) => updatePrescriptionItem(i, 'duration', v)} placeholder="5 días" />
          </div>
        </div>
      ))}
    </div>
  );
}

function Step3LabOrders() {
  const { wizard, addLabStudy, removeLabStudy, updateLabStudy } = useClinicalStore();
  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h3 className="font-semibold text-gray-800">Paso 3 — Estudios de Laboratorio</h3>
        <button onClick={addLabStudy} className="text-sm text-purple-600 hover:text-purple-800 font-medium">+ Agregar estudio</button>
      </div>
      {wizard.labStudies.length === 0 && (
        <p className="text-sm text-gray-400 italic">Sin estudios solicitados (opcional)</p>
      )}
      {wizard.labStudies.map((study, i) => (
        <div key={i} className="border border-gray-200 rounded-lg p-3 space-y-2">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-gray-500">Estudio {i + 1}</span>
            <button onClick={() => removeLabStudy(i)} className="text-red-500 text-xs hover:text-red-700">Eliminar</button>
          </div>
          <div className="grid grid-cols-2 gap-2">
            <TextInput label="Tipo de estudio" value={study.studyType} onChange={(v) => updateLabStudy(i, 'studyType', v)} placeholder="Biometría hemática" />
            <select value={study.priority} onChange={(e) => updateLabStudy(i, 'priority', e.target.value)}
              className="border border-gray-300 rounded-lg px-3 py-2 text-sm mt-5">
              <option value="ROUTINE">Rutina</option>
              <option value="URGENT">Urgente</option>
            </select>
          </div>
          <TextInput label="Instrucciones" value={study.instructions} onChange={(v) => updateLabStudy(i, 'instructions', v)} placeholder="Ayuno de 8 horas" />
        </div>
      ))}
    </div>
  );
}

function Step4Review() {
  const { wizard, activePatient } = useClinicalStore();
  return (
    <div className="space-y-4">
      <h3 className="font-semibold text-gray-800">Paso 4 — Revisión</h3>
      <div className="bg-gray-50 rounded-lg p-4 space-y-3 text-sm">
        <p><strong>Paciente:</strong> {activePatient?.fullName}</p>
        <p><strong>Diagnóstico:</strong> {wizard.diagnosis} {wizard.diagnosisCode && `(${wizard.diagnosisCode})`}</p>
        <p><strong>Plan:</strong> {wizard.plan}</p>
        {wizard.prescriptionItems.length > 0 && (
          <div>
            <strong>Receta:</strong> {wizard.prescriptionItems.length} medicamento(s)
            <ul className="ml-4 list-disc text-gray-600">
              {wizard.prescriptionItems.map((item, i) => (
                <li key={i}>{item.medicationName} — {item.dosage} {item.frequency}</li>
              ))}
            </ul>
          </div>
        )}
        {wizard.labStudies.length > 0 && (
          <div>
            <strong>Laboratorio:</strong> {wizard.labStudies.length} estudio(s)
            <ul className="ml-4 list-disc text-gray-600">
              {wizard.labStudies.map((s, i) => (
                <li key={i}>{s.studyType} ({s.priority})</li>
              ))}
            </ul>
          </div>
        )}
        {wizard.requiresSupervision && <p className="text-amber-600 font-medium">Requiere supervisión</p>}
      </div>
    </div>
  );
}

function TextInput({ label, value, onChange, placeholder, required }: {
  label: string; value: string; onChange: (v: string) => void; placeholder?: string; required?: boolean;
}) {
  return (
    <label className="block">
      <span className="text-xs font-medium text-gray-600">{label}</span>
      <input type="text" value={value} onChange={(e) => onChange(e.target.value)} placeholder={placeholder} required={required}
        className="mt-1 block w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent" />
    </label>
  );
}

function Textarea({ label, value, onChange, placeholder, required }: {
  label: string; value: string; onChange: (v: string) => void; placeholder?: string; required?: boolean;
}) {
  return (
    <label className="block">
      <span className="text-xs font-medium text-gray-600">{label}</span>
      <textarea value={value} onChange={(e) => onChange(e.target.value)} placeholder={placeholder} required={required} rows={3}
        className="mt-1 block w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent resize-none" />
    </label>
  );
}
