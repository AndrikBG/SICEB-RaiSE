import { Client, type IMessage, type StompSubscription } from '@stomp/stompjs';
import { useAuthStore } from '@/stores/auth-store';
import { useSyncStore } from '@/stores/sync-store';

const WS_URL = import.meta.env.VITE_WS_URL ?? 'ws://localhost:8080/ws';

let stompClient: Client | null = null;

export function getStompClient(): Client {
  if (stompClient) return stompClient;

  stompClient = new Client({
    brokerURL: WS_URL,
    reconnectDelay: 5_000,
    heartbeatIncoming: 10_000,
    heartbeatOutgoing: 10_000,

    beforeConnect: (client) => {
      const token = useAuthStore.getState().accessToken;
      if (token) {
        client.connectHeaders = { Authorization: `Bearer ${token}` };
      }
    },

    onConnect: () => {
      useSyncStore.getState().setConnectionStatus('online');
      console.info('[WS] Connected');
    },

    onDisconnect: () => {
      useSyncStore.getState().setConnectionStatus('offline');
      console.info('[WS] Disconnected');
    },

    onStompError: (frame) => {
      console.error('[WS] STOMP error:', frame.headers.message);
    },

    onWebSocketClose: () => {
      useSyncStore.getState().setConnectionStatus('reconnecting');
    },
  });

  return stompClient;
}

export function connectWs(): void {
  getStompClient().activate();
}

export function disconnectWs(): void {
  stompClient?.deactivate();
  stompClient = null;
}

export function subscribeTopic(
  destination: string,
  callback: (msg: IMessage) => void,
): StompSubscription | undefined {
  const client = getStompClient();
  if (!client.connected) {
    console.warn('[WS] Cannot subscribe — not connected');
    return undefined;
  }
  return client.subscribe(destination, callback);
}

export function publishMessage(destination: string, body: unknown): void {
  const client = getStompClient();
  if (!client.connected) {
    console.warn('[WS] Cannot publish — not connected');
    return;
  }
  client.publish({ destination, body: JSON.stringify(body) });
}
