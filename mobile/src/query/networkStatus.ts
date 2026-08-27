import type { NetworkState } from 'expo-network';

export function isNetworkOnline(
  state: Pick<NetworkState, 'isConnected' | 'isInternetReachable'>,
): boolean {
  return state.isConnected === true && state.isInternetReachable !== false;
}
