import { useEffect } from 'react';
import { useSyncStore } from '@/stores/sync-store';

export function useOnlineStatus() {
  const setConnectionStatus = useSyncStore((s) => s.setConnectionStatus);

  useEffect(() => {
    const handleOnline = () => setConnectionStatus('online');
    const handleOffline = () => setConnectionStatus('offline');

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, [setConnectionStatus]);

  return useSyncStore((s) => s.connectionStatus);
}
