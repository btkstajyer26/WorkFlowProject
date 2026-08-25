import * as SecureStore from 'expo-secure-store';
import { Platform } from 'react-native';

const REFRESH_TOKEN_KEY = 'ebys.auth.refresh-token';
const SECURE_STORE_OPTIONS: SecureStore.SecureStoreOptions = {
  keychainAccessible: SecureStore.WHEN_UNLOCKED_THIS_DEVICE_ONLY,
};

let accessTokenInMemory: string | null = null;

function getWebStorage(): Storage | null {
  if (typeof localStorage === 'undefined') return null;
  return localStorage;
}

export function getAccessToken(): string | null {
  return accessTokenInMemory;
}

export function setAccessToken(accessToken: string | null): void {
  accessTokenInMemory = accessToken;
}

export async function getRefreshToken(): Promise<string | null> {
  if (Platform.OS === 'web') {
    return getWebStorage()?.getItem(REFRESH_TOKEN_KEY) ?? null;
  }

  return SecureStore.getItemAsync(REFRESH_TOKEN_KEY, SECURE_STORE_OPTIONS);
}

export async function setRefreshToken(refreshToken: string): Promise<void> {
  if (Platform.OS === 'web') {
    getWebStorage()?.setItem(REFRESH_TOKEN_KEY, refreshToken);
    return;
  }

  await SecureStore.setItemAsync(REFRESH_TOKEN_KEY, refreshToken, SECURE_STORE_OPTIONS);
}

export async function saveSessionTokens(tokens: {
  accessToken: string;
  refreshToken: string;
}): Promise<void> {
  await setRefreshToken(tokens.refreshToken);
  setAccessToken(tokens.accessToken);
}

export async function clearSessionTokens(): Promise<void> {
  setAccessToken(null);

  if (Platform.OS === 'web') {
    getWebStorage()?.removeItem(REFRESH_TOKEN_KEY);
    return;
  }

  await SecureStore.deleteItemAsync(REFRESH_TOKEN_KEY, SECURE_STORE_OPTIONS);
}
