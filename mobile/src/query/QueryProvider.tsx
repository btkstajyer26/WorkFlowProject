import {
  focusManager,
  onlineManager,
  QueryClient,
  QueryClientProvider,
} from '@tanstack/react-query';
import * as Network from 'expo-network';
import {
  createContext,
  type PropsWithChildren,
  useContext,
  useEffect,
  useState,
} from 'react';
import { AppState, type AppStateStatus, Platform } from 'react-native';

import { ApiClientError } from '@/api/errors';
import { isNetworkOnline } from './networkStatus';

const NetworkStatusContext = createContext<boolean | null>(null);

export const queryClient = new QueryClient({
  defaultOptions: {
    mutations: {
      retry: false,
    },
    queries: {
      gcTime: 5 * 60 * 1000,
      refetchOnReconnect: true,
      retry: (failureCount, error) =>
        error instanceof ApiClientError &&
        (error.status === 0 || error.status >= 500) &&
        failureCount < 2,
      staleTime: 30 * 1000,
    },
  },
});

function updateFocus(status: AppStateStatus): void {
  if (Platform.OS !== 'web') {
    focusManager.setFocused(status === 'active');
  }
}

export function QueryProvider({ children }: PropsWithChildren) {
  const [isOffline, setIsOffline] = useState(false);

  useEffect(() => {
    updateFocus(AppState.currentState);
    const subscription = AppState.addEventListener('change', updateFocus);

    return () => subscription.remove();
  }, []);

  useEffect(() => {
    let isMounted = true;
    let receivedNetworkEvent = false;

    const updateNetworkStatus = (state: Network.NetworkState) => {
      if (!isMounted) return;

      const isOnline = isNetworkOnline(state);
      onlineManager.setOnline(isOnline);
      setIsOffline(!isOnline);
    };

    const subscription = Network.addNetworkStateListener((state) => {
      receivedNetworkEvent = true;
      updateNetworkStatus(state);
    });

    void Network.getNetworkStateAsync()
      .then((state) => {
        if (!receivedNetworkEvent) updateNetworkStatus(state);
      })
      .catch(() => {
        // Ağ durumu okunamazsa mevcut durumu koru; API hataları ayrıca ele alınır.
      });

    return () => {
      isMounted = false;
      subscription.remove();
    };
  }, []);

  return (
    <QueryClientProvider client={queryClient}>
      <NetworkStatusContext.Provider value={isOffline}>
        {children}
      </NetworkStatusContext.Provider>
    </QueryClientProvider>
  );
}

export function useNetworkStatus(): boolean {
  const isOffline = useContext(NetworkStatusContext);

  if (isOffline === null) {
    throw new Error('useNetworkStatus, QueryProvider içinde kullanılmalıdır.');
  }

  return isOffline;
}
