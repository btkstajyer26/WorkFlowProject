import Constants, { ExecutionEnvironment } from 'expo-constants';
import * as Device from 'expo-device';
import { Platform } from 'react-native';

import { registerDeviceToken, type DevicePlatform } from '@/api/deviceTokens';

const isExpoGo =
  Constants.executionEnvironment === ExecutionEnvironment.StoreClient;

function getNotificationsModule() {
  if (isExpoGo) return null;
  try {
    // eslint-disable-next-line @typescript-eslint/no-require-imports
    return require('expo-notifications');
  } catch {
    return null;
  }
}

const Notifications = getNotificationsModule();
if (Notifications) {
  try {
    Notifications.setNotificationHandler({
      handleNotification: async () => ({
        shouldPlaySound: true,
        shouldSetBadge: true,
        shouldShowBanner: true,
        shouldShowList: true,
      }),
    });
  } catch {
    // Ignore in unsupported environments
  }
}

let cachedDeviceToken: string | null = null;

function maskDeviceToken(token: string): string {
  const visibleSuffix = token.slice(-6);
  return `***${visibleSuffix}`;
}

export function getCachedDeviceToken(): string | null {
  return cachedDeviceToken;
}

export function setCachedDeviceToken(token: string | null): void {
  cachedDeviceToken = token;
}

export async function registerPushTokenWithBackend(): Promise<string | null> {
  if (!Device.isDevice || isExpoGo) {
    return null;
  }

  const Notifications = getNotificationsModule();
  if (!Notifications) return null;

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

    if (Platform.OS === 'android') {
      await Notifications.setNotificationChannelAsync('default', {
        name: 'Genel Bildirimler',
        importance: Notifications.AndroidImportance.MAX,
        vibrationPattern: [0, 250, 250, 250],
        lightColor: '#7137dc',
      });
    }

    // Android/iOS native push token (FCM / APNs)
    const tokenResult = await Notifications.getDevicePushTokenAsync();
    const token = tokenResult?.data;

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

    console.log('[Push] Token başarıyla kaydedildi:', maskDeviceToken(token));
    return token;
  } catch (error) {
    console.warn('[Push] Push token kaydı tamamlanamadı:', error);
    return null;
  }
}

export function subscribeToNotificationResponses(
  onNavigateToRecord: (recordId: string) => void,
) {
  const Notifications = getNotificationsModule();
  if (!Notifications) {
    return () => {};
  }

  try {
    const subscription =
      Notifications.addNotificationResponseReceivedListener((response: any) => {
        const data = response?.notification?.request?.content?.data;
        const recordId = data?.recordId;
        if (typeof recordId === 'string' && recordId.trim()) {
          onNavigateToRecord(recordId.trim());
        }
      });

    return () => {
      subscription.remove();
    };
  } catch {
    return () => {};
  }
}

export function subscribeToNotificationReceived(
  onReceived: (notification: any) => void,
) {
  const Notifications = getNotificationsModule();
  if (!Notifications) {
    return () => {};
  }

  try {
    const subscription =
      Notifications.addNotificationReceivedListener(onReceived);

    return () => {
      subscription.remove();
    };
  } catch {
    return () => {};
  }
}
