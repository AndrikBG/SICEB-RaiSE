import { Link } from 'react-router-dom';

export function ConsultationsLandingView() {
  return (
    <div className="rounded-lg border border-gray-200 bg-white p-6 shadow-sm">
      <h1 className="text-xl font-semibold text-gray-900">Consultas</h1>
      <p className="mt-2 text-gray-600">
        Cada consulta se registra desde el expediente del paciente: busque al paciente, abra su
        expediente y use el flujo de nueva consulta.
      </p>
      <Link
        to="/patients"
        className="mt-4 inline-block rounded-md bg-purple-600 px-4 py-2 text-sm font-medium text-white hover:bg-purple-700"
      >
        Ir a pacientes
      </Link>
    </div>
  );
}
