import { api } from './api-client';

/** Contexto clínico va en el JWT (TenantFilter); no importar auth-store aquí para evitar ciclos de módulos. */
function clinicalHeaders(): Record<string, string> {
  return {};
}

export interface CreatePatientRequest {
  patientId: string;
  firstName: string;
  paternalSurname: string;
  maternalSurname?: string;
  dateOfBirth: string;
  gender: 'MALE' | 'FEMALE' | 'OTHER';
  phone?: string;
  curp?: string;
  patientType: 'STUDENT' | 'WORKER' | 'EXTERNAL';
  credentialNumber?: string;
  guardianName?: string;
  guardianRelationship?: string;
  guardianPhone?: string;
  guardianIdConfirmed: boolean;
  dataConsentGiven: boolean;
  specialCase: boolean;
  specialCaseNotes?: string;
  idempotencyKey: string;
}

export interface ConsultationRequest {
  consultationId: string;
  recordId: string;
  subjective: string;
  objective: string;
  diagnosis: string;
  diagnosisCode?: string;
  plan: string;
  vitalSigns?: string;
  requiresSupervision: boolean;
  supervisorStaffId?: string;
  idempotencyKey: string;
}

export interface PrescriptionItem {
  medicationId: string;
  medicationName: string;
  quantity: number;
  dosage: string;
  frequency: string;
  duration?: string;
  route?: string;
  instructions?: string;
  isControlled?: boolean; // Phase 5: validated against medication catalog
}

export interface PrescriptionRequest {
  prescriptionId: string;
  consultationId: string;
  recordId: string;
  items: PrescriptionItem[];
  idempotencyKey: string;
}

export interface LabStudyItem {
  studyId: string;
  studyType: string;
  priority?: string;
  instructions?: string;
}

export interface LabStudiesRequest {
  consultationId: string;
  recordId: string;
  studies: LabStudyItem[];
  idempotencyKey: string;
}

export interface LabResultRequest {
  studyId: string;
  resultId: string;
  resultText: string;
  idempotencyKey: string;
}

export interface PatientSearchResult {
  patientId: string;
  fullName: string;
  dateOfBirth: string;
  patientType: string;
  gender: string;
  phone: string | null;
  curp?: string | null;
  credentialNumber?: string | null;
  profileStatus: string;
  branchId: string;
  recordId: string | null;
  lastVisitDate: string | null;
  consultationCount: number;
}

export interface DuplicatePatient {
  patientId: string;
  fullName: string;
  dateOfBirth: string;
}

export interface TimelineEntry {
  eventId: string;
  eventType: string;
  occurredAt: string;
  performedByStaffId: string;
  summary: string;
  payload: Record<string, unknown>;
}

export interface PendingLabStudy {
  studyId: string;
  eventId: string;
  recordId: string;
  patientId: string;
  patientName: string;
  consultationId: string | null;
  studyType: string;
  priority: string;
  status: string;
  instructions: string | null;
  requestedAt: string;
  resultText: string | null;
}

export const clinicalApi = {
  createPatient: (data: CreatePatientRequest) =>
    api.post<{ patientId: string; recordId: string }>('/api/patients', data, { headers: clinicalHeaders() }),

  addConsultation: (data: ConsultationRequest) =>
    api.post<{ consultationId: string }>('/api/consultations', data, { headers: clinicalHeaders() }),

  createPrescription: (consultationId: string, data: PrescriptionRequest) =>
    api.post<{ prescriptionId: string }>(`/api/consultations/${consultationId}/prescriptions`, data, { headers: clinicalHeaders() }),

  createLabStudies: (consultationId: string, data: LabStudiesRequest) =>
    api.post<{ studyIds: string[] }>(`/api/consultations/${consultationId}/lab-studies`, data, { headers: clinicalHeaders() }),

  recordLabResult: (studyId: string, data: LabResultRequest) =>
    api.post<{ resultId: string }>(`/api/lab-studies/${studyId}/results`, data, { headers: clinicalHeaders() }),

  searchPatients: (params: { q?: string; dateOfBirth?: string; type?: string; page?: number; size?: number }) =>
    api.get<{ content: PatientSearchResult[]; totalElements: number; totalPages: number }>(
      '/api/patients/search', { params, headers: clinicalHeaders() }),

  findPotentialDuplicates: (params: {
    firstName: string;
    paternalSurname: string;
    maternalSurname?: string;
    dateOfBirth: string;
  }) =>
    api.get<DuplicatePatient[]>('/api/patients/duplicates', { params, headers: clinicalHeaders() }),

  getTimeline: (patientId: string, page = 0, size = 20) =>
    api.get<{ content: TimelineEntry[]; totalElements: number; totalPages: number }>(
      `/api/patients/${patientId}/timeline`, { params: { page, size }, headers: clinicalHeaders() }),

  getNom004: (patientId: string) =>
    api.get(`/api/patients/${patientId}/nom004`, { headers: clinicalHeaders() }),

  getPendingLabStudies: (status?: string) =>
    api.get<PendingLabStudy[]>('/api/lab-studies/pending', { params: status ? { status } : {}, headers: clinicalHeaders() }),
};
