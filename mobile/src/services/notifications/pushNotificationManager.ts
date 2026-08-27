import Constants, { ExecutionEnvironment } from 'expo-constants';
import * as Device from 'expo-device';
import * as Notifications from 'expo-notifications';
import { Platform } from 'react-native';

import { registerDeviceToken, type DevicePlatform } from '@/api/deviceTokens';

Notifications.setNotificationHandler({
  handleNotification: async () => ({
    shouldPlaySound: true,
    shouldSetBadge: true,
    shouldShowAlert: true,
    shouldShowBanner: true,
    shouldShowList: true,
  }),
});

let cachedDeviceToken: string | null = null;

export function getCachedDeviceToken(): string | null {
  return cachedDeviceToken;
}

export function setCachedDeviceToken(token: string | null): void {
  cachedDeviceToken = token;
}

export async function registerPushTokenWithBackend(): Promise<string | null> {
  const isExpoGo =
    Constants.executionEnvironment === ExecutionEnvironment.StoreClient;

  if (!Device.isDevice || isExpoGo) {
    return null;
  }

  try {
    const { status: existingStatus } =
      await Notifications.getPermissionsAsync();
    let finalStatus = existingStatus;

    if (existingStatus !== 'granted') {
      const { status } = await Notifications.requestPermissionsAsync();
      finalStatus = status;
    }

    if (finalStatus !== 'granted') {
      return null;
    }

    // Android/iOS native push token (FCM / APNs)
    const tokenResult = await Notifications.getDevicePushTokenAsync();
    const token = tokenResult.data;

    if (!token) return null;

    cachedDeviceToken = token;

    const platform: DevicePlatform =
      Platform.OS === 'ios' ? 'IOS' : 'ANDROID';
    const deviceName = Device.modelName || Device.deviceName || undefined;

    await registerDeviceToken({
      deviceName,
      platform,
      token,
    });

    return token;
  } catch {
    // Push token alınamazsa veya ağ hatası olursa akışı kesme
    return null;
  }
}

export function subscribeToNotificationResponses(
  onNavigateToRecord: (recordId: string) => void,
) {
  const subscription =
    Notifications.addNotificationResponseReceivedListener((response) => {
      const data = response.notification.request.content.data;
      const recordId = data?.recordId;
      if (typeof recordId === 'string' && recordId.trim()) {
        onNavigateToRecord(recordId.trim());
      }
    });

  return () => {
    subscription.remove();
  };
}

export function subscribeToNotificationReceived(
  onReceived: (notification: Notifications.Notification) => void,
) {
  const subscription =
    Notifications.addNotificationReceivedListener(onReceived);

  return () => {
    subscription.remove();
  };
}
