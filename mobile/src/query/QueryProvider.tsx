import {
  focusManager,
  onlineManager,
  QueryClient,
  QueryClientProvider,
} from '@tanstack/react-query';
import * as Network from 'expo-network';
import { type PropsWithChildren, useEffect } from 'react';
import { AppState, type AppStateStatus, Platform } from 'react-native';

import { ApiClientError } from '@/api/errors';

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
  useEffect(() => {
    updateFocus(AppState.currentState);
    const subscription = AppState.addEventListener('change', updateFocus);

    return () => subscription.remove();
  }, []);

  useEffect(() => {
    const subscription = Network.addNetworkStateListener((state) => {
      const isOnline = state.isConnected === true && state.isInternetReachable !== false;
      onlineManager.setOnline(isOnline);
    });

    return () => subscription.remove();
  }, []);

  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}
