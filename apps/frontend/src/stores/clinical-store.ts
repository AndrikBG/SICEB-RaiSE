import { create } from 'zustand';
import type { PatientSearchResult, TimelineEntry, PendingLabStudy } from '@/lib/clinical-api';

interface WizardState {
  step: number;
  recordId: string | null;
  consultationId: string | null;
  subjective: string;
  objective: string;
  diagnosis: string;
  diagnosisCode: string;
  plan: string;
  vitalSigns: string;
  requiresSupervision: boolean;
  prescriptionItems: Array<{
    medicationId: string;
    medicationName: string;
    quantity: number;
    dosage: string;
    frequency: string;
    duration: string;
    route: string;
    instructions: string;
  }>;
  labStudies: Array<{
    studyId: string;
    studyType: string;
    priority: string;
    instructions: string;
  }>;
}

interface ClinicalState {
  activePatient: PatientSearchResult | null;
  timeline: TimelineEntry[];
  pendingLabStudies: PendingLabStudy[];
  searchQuery: string;
  searchResults: PatientSearchResult[];
  isLoading: boolean;
  wizard: WizardState;

  setActivePatient: (patient: PatientSearchResult | null) => void;
  setTimeline: (entries: TimelineEntry[]) => void;
  setPendingLabStudies: (studies: PendingLabStudy[]) => void;
  setSearchQuery: (query: string) => void;
  setSearchResults: (results: PatientSearchResult[]) => void;
  setLoading: (loading: boolean) => void;

  setWizardStep: (step: number) => void;
  updateWizard: (partial: Partial<WizardState>) => void;
  resetWizard: () => void;
  addPrescriptionItem: () => void;
  removePrescriptionItem: (index: number) => void;
  updatePrescriptionItem: (index: number, field: string, value: string | number) => void;
  addLabStudy: () => void;
  removeLabStudy: (index: number) => void;
  updateLabStudy: (index: number, field: string, value: string) => void;
}

const initialWizard: WizardState = {
  step: 0,
  recordId: null,
  consultationId: null,
  subjective: '',
  objective: '',
  diagnosis: '',
  diagnosisCode: '',
  plan: '',
  vitalSigns: '',
  requiresSupervision: false,
  prescriptionItems: [],
  labStudies: [],
};

export const useClinicalStore = create<ClinicalState>()((set) => ({
  activePatient: null,
  timeline: [],
  pendingLabStudies: [],
  searchQuery: '',
  searchResults: [],
  isLoading: false,
  wizard: { ...initialWizard },

  setActivePatient: (patient) => set({ activePatient: patient }),
  setTimeline: (entries) => set({ timeline: entries }),
  setPendingLabStudies: (studies) => set({ pendingLabStudies: studies }),
  setSearchQuery: (query) => set({ searchQuery: query }),
  setSearchResults: (results) => set({ searchResults: results }),
  setLoading: (loading) => set({ isLoading: loading }),

  setWizardStep: (step) => set((s) => ({ wizard: { ...s.wizard, step } })),
  updateWizard: (partial) => set((s) => ({ wizard: { ...s.wizard, ...partial } })),
  resetWizard: () => set({ wizard: { ...initialWizard } }),

  addPrescriptionItem: () =>
    set((s) => ({
      wizard: {
        ...s.wizard,
        prescriptionItems: [
          ...s.wizard.prescriptionItems,
          { medicationId: crypto.randomUUID(), medicationName: '', quantity: 1, dosage: '', frequency: '', duration: '', route: 'ORAL', instructions: '' },
        ],
      },
    })),

  removePrescriptionItem: (index) =>
    set((s) => ({
      wizard: {
        ...s.wizard,
        prescriptionItems: s.wizard.prescriptionItems.filter((_, i) => i !== index),
      },
    })),

  updatePrescriptionItem: (index, field, value) =>
    set((s) => ({
      wizard: {
        ...s.wizard,
        prescriptionItems: s.wizard.prescriptionItems.map((item, i) =>
          i === index ? { ...item, [field]: value } : item
        ),
      },
    })),

  addLabStudy: () =>
    set((s) => ({
      wizard: {
        ...s.wizard,
        labStudies: [
          ...s.wizard.labStudies,
          { studyId: crypto.randomUUID(), studyType: '', priority: 'ROUTINE', instructions: '' },
        ],
      },
    })),

  removeLabStudy: (index) =>
    set((s) => ({
      wizard: {
        ...s.wizard,
        labStudies: s.wizard.labStudies.filter((_, i) => i !== index),
      },
    })),

  updateLabStudy: (index, field, value) =>
    set((s) => ({
      wizard: {
        ...s.wizard,
        labStudies: s.wizard.labStudies.map((item, i) =>
          i === index ? { ...item, [field]: value } : item
        ),
      },
    })),
}));
