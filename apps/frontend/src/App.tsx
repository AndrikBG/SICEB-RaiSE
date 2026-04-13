import { Routes, Route, Navigate } from 'react-router-dom';
import { Layout } from '@/components/layout';
import { ProtectedRoute } from '@/components/ProtectedRoute';
import { LoginView } from '@/features/auth/LoginView';
import { BranchSelectionView } from '@/features/auth/BranchSelectionView';
import { ChangePasswordView } from '@/features/auth/ChangePasswordView';
import { PatientSearchView } from '@/features/clinical/PatientSearchView';
import { MedicalRecordView } from '@/features/clinical/MedicalRecordView';
import { ConsultationWizard } from '@/features/clinical/ConsultationWizard';
import { ConsultationsLandingView } from '@/features/clinical/ConsultationsLandingView';
import { PendingLabStudiesView } from '@/features/clinical/PendingLabStudiesView';
import { LabResultEntryForm } from '@/features/clinical/LabResultEntryForm';
import { UserManagementView } from '@/features/admin/UserManagementView';
import { RoleConfigurationView } from '@/features/admin/RoleConfigurationView';
import { BranchManagementView } from '@/features/operations/BranchManagementView';
import { InventoryDashboardView } from '@/features/operations/InventoryDashboardView';
import { ServiceInventoryView } from '@/features/operations/ServiceInventoryView';
import { TariffConfigurationView } from '@/features/operations/TariffConfigurationView';

function App() {
  return (
    <Routes>
      {/* Public routes */}
      <Route path="/login" element={<LoginView />} />
      <Route path="/select-branch" element={<BranchSelectionView />} />
      <Route path="/change-password" element={<ChangePasswordView />} />

      {/* Protected routes */}
      <Route element={<ProtectedRoute />}>
        <Route element={<Layout />}>
          <Route path="/" element={<Navigate to="/patients" replace />} />
          <Route path="/consultations" element={<ConsultationsLandingView />} />
          <Route path="/patients" element={<PatientSearchView />} />
          <Route path="/patients/:patientId" element={<MedicalRecordView />} />
          <Route path="/patients/:patientId/consultation" element={<ConsultationWizard />} />
          <Route path="/lab" element={<PendingLabStudiesView />} />
          <Route path="/lab/:studyId/result" element={<LabResultEntryForm />} />
          <Route path="/branches" element={<BranchManagementView />} />
          <Route path="/inventory" element={<InventoryDashboardView />} />
          <Route path="/inventory/service" element={<ServiceInventoryView />} />
          <Route path="/tariffs" element={<TariffConfigurationView />} />
          <Route path="/admin/users" element={<UserManagementView />} />
          <Route path="/admin/roles" element={<RoleConfigurationView />} />
        </Route>
      </Route>
    </Routes>
  );
}

export default App;
