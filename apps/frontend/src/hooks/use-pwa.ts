import { useRegisterSW } from 'virtual:pwa-register/react';

export function usePwa() {
  const {
    needRefresh: [needRefresh, setNeedRefresh],
    offlineReady: [offlineReady, setOfflineReady],
    updateServiceWorker,
  } = useRegisterSW({
    onRegisteredSW(swUrl, registration) {
      if (registration) {
        setInterval(() => registration.update(), 60 * 60 * 1000);
      }
      console.info('[PWA] Service Worker registered:', swUrl);
    },
    onRegisterError(error) {
      console.error('[PWA] Service Worker registration error:', error);
    },
  });

  function close() {
    setOfflineReady(false);
    setNeedRefresh(false);
  }

  return { offlineReady, needRefresh, updateServiceWorker, close };
}
