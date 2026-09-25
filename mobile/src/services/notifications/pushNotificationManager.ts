import Constants, { ExecutionEnvironment } from 'expo-constants';
import * as Device from 'expo-device';
import type {
  DevicePushToken,
  NotificationResponse,
} from 'expo-notifications';
import { Platform } from 'react-native';
import { z } from 'zod';

import { registerDeviceToken, type DevicePlatform } from '@/api/deviceTokens';

const isExpoGo =
  Constants.executionEnvironment === ExecutionEnvironment.StoreClient;

type NotificationsModule = typeof import('expo-notifications');

const recordIdSchema = z.string().trim().uuid();

function getNotificationsModule(): NotificationsModule | null {
  if (isExpoGo) return null;
  try {
    // eslint-disable-next-line @typescript-eslint/no-require-imports
    return require('expo-notifications') as NotificationsModule;
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
const pendingDeviceTokens = new Set<string>();

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

function getDevicePlatform(): DevicePlatform | null {
  if (Platform.OS === 'android') return 'ANDROID';
  if (Platform.OS === 'ios') return 'IOS';
  return null;
}

function normalizeNativeToken(token: unknown): string | null {
  return typeof token === 'string' && token.trim() ? token.trim() : null;
}

async function registerNativeTokenWithBackend(
  rawToken: unknown,
): Promise<string | null> {
  const token = normalizeNativeToken(rawToken);
  const platform = getDevicePlatform();

  if (!token || !platform) return null;
  if (token === cachedDeviceToken || pendingDeviceTokens.has(token)) {
    return token;
  }

  pendingDeviceTokens.add(token);

  try {
    const deviceName = Device.modelName || Device.deviceName || undefined;

    await registerDeviceToken({
      deviceName,
      platform,
      token,
    });

    cachedDeviceToken = token;
    console.log('[Push] Token başarıyla kaydedildi:', maskDeviceToken(token));
    return token;
  } catch (error) {
    console.warn('[Push] Push token kaydı tamamlanamadı:', error);
    return null;
  } finally {
    pendingDeviceTokens.delete(token);
  }
}

export async function registerPushTokenWithBackend(): Promise<string | null> {
  if (!Device.isDevice || isExpoGo) {
    return null;
  }

  const Notifications = getNotificationsModule();
  if (!Notifications) return null;

  try {
    if (Platform.OS === 'android') {
      await Notifications.setNotificationChannelAsync('default', {
        name: 'Genel Bildirimler',
        importance: Notifications.AndroidImportance.MAX,
        vibrationPattern: [0, 250, 250, 250],
        lightColor: '#7137dc',
      });
    }

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
    return registerNativeTokenWithBackend(tokenResult?.data);
  } catch (error) {
    console.warn('[Push] Push token kaydı tamamlanamadı:', error);
    return null;
  }
}

export function subscribeToPushTokenChanges(): () => void {
  if (!Device.isDevice || isExpoGo) {
    return () => {};
  }

  const Notifications = getNotificationsModule();
  if (!Notifications) return () => {};

  try {
    const subscription = Notifications.addPushTokenListener(
      (tokenResult: DevicePushToken) => {
        void registerNativeTokenWithBackend(tokenResult?.data);
      },
    );

    return () => {
      subscription.remove();
    };
  } catch {
    return () => {};
  }
}

function getNotificationResponseKey(
  response: NotificationResponse,
): string | null {
  const identifier = response?.notification?.request?.identifier;
  return typeof identifier === 'string' && identifier.trim()
    ? identifier
    : null;
}

export function subscribeToNotificationResponses(
  onNavigateToRecord: (recordId: string) => void,
) {
  const Notifications = getNotificationsModule();
  if (!Notifications) {
    return () => {};
  }

  let subscription: { remove: () => void } | null = null;

  try {
    const handledResponses = new Set<string>();
    const handleResponse = (response: NotificationResponse | null) => {
      if (!response) return;

      const responseKey = getNotificationResponseKey(response);
      if (responseKey && handledResponses.has(responseKey)) return;
      if (responseKey) handledResponses.add(responseKey);

      const result = recordIdSchema.safeParse(
        response.notification.request.content.data?.recordId,
      );
      if (result.success) {
        onNavigateToRecord(result.data);
      }

      try {
        Notifications.clearLastNotificationResponse();
      } catch {
        // Response consumption must not crash unsupported native environments.
      }
    };

    subscription = Notifications.addNotificationResponseReceivedListener(
      handleResponse,
    );
    handleResponse(Notifications.getLastNotificationResponse());

    return () => {
      subscription?.remove();
    };
  } catch {
    subscription?.remove();
    return () => {};
  }
}
