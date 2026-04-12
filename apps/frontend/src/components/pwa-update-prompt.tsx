import { usePwa } from '@/hooks/use-pwa';

export function PwaUpdatePrompt() {
  const { offlineReady, needRefresh, updateServiceWorker, close } = usePwa();

  if (!offlineReady && !needRefresh) return null;

  return (
    <div className="fixed bottom-4 right-4 z-50 max-w-sm rounded-lg border border-gray-200 bg-white p-4 shadow-lg">
      {offlineReady && (
        <p className="text-sm text-gray-700">
          La aplicación está lista para uso sin conexión.
        </p>
      )}
      {needRefresh && (
        <p className="text-sm text-gray-700">
          Hay una nueva versión disponible.
        </p>
      )}
      <div className="mt-3 flex gap-2">
        {needRefresh && (
          <button
            type="button"
            onClick={() => updateServiceWorker()}
            className="rounded-md bg-purple-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-purple-700"
          >
            Actualizar
          </button>
        )}
        <button
          type="button"
          onClick={close}
          className="rounded-md border border-gray-300 px-3 py-1.5 text-sm font-medium text-gray-700 hover:bg-gray-50"
        >
          Cerrar
        </button>
      </div>
    </div>
  );
}
