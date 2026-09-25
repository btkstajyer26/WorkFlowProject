import * as Notifications from 'expo-notifications';
import { Platform } from 'react-native';

import { registerDeviceToken } from '@/api/deviceTokens';
import {
  getCachedDeviceToken,
  registerPushTokenWithBackend,
  setCachedDeviceToken,
  subscribeToNotificationResponses,
  subscribeToPushTokenChanges,
} from './pushNotificationManager';

let mockForegroundHandler:
  | {
      handleNotification: () => Promise<{
        shouldPlaySound: boolean;
        shouldSetBadge: boolean;
        shouldShowBanner: boolean;
        shouldShowList: boolean;
      }>;
    }
  | undefined;
let mockIsDevice = true;

jest.mock('expo-device', () => ({
  get deviceName() {
    return 'Test Phone';
  },
  get isDevice() {
    return mockIsDevice;
  },
  get modelName() {
    return 'Galaxy S21';
  },
}));

jest.mock('expo-notifications', () => ({
  AndroidImportance: { MAX: 'max' },
  addNotificationResponseReceivedListener: jest
    .fn()
    .mockReturnValue({ remove: jest.fn() }),
  addPushTokenListener: jest.fn().mockReturnValue({ remove: jest.fn() }),
  clearLastNotificationResponse: jest.fn(),
  getDevicePushTokenAsync: jest.fn(),
  getLastNotificationResponse: jest.fn(),
  getPermissionsAsync: jest.fn(),
  requestPermissionsAsync: jest.fn(),
  setNotificationChannelAsync: jest.fn(),
  setNotificationHandler: jest.fn((handler) => {
    mockForegroundHandler = handler;
  }),
}));

jest.mock('@/api/deviceTokens', () => ({
  registerDeviceToken: jest.fn(),
}));

const validRecordId = 'd3b07384-d113-4632-8fe2-51a6597a7a58';

function notificationResponse(
  recordId: unknown,
  identifier = 'notification-1',
) {
  return {
    actionIdentifier: Notifications.DEFAULT_ACTION_IDENTIFIER,
    notification: {
      date: Date.now(),
      request: {
        content: {
          data: { recordId },
        },
        identifier,
        trigger: null,
      },
    },
  } as unknown as Notifications.NotificationResponse;
}

describe('pushNotificationManager', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    setCachedDeviceToken(null);
    mockIsDevice = true;
    (Notifications.getLastNotificationResponse as jest.Mock).mockReturnValue(
      null,
    );
  });

  it('fiziksel cihaz değilse token istemez ve null döner', async () => {
    mockIsDevice = false;

    const result = await registerPushTokenWithBackend();

    expect(result).toBeNull();
    expect(Notifications.getPermissionsAsync).not.toHaveBeenCalled();
  });

  it('ilk cihaz tokenını mevcut platformla backend e kaydeder', async () => {
    const consoleLogSpy = jest
      .spyOn(console, 'log')
      .mockImplementation(() => {});
    (Notifications.getPermissionsAsync as jest.Mock).mockResolvedValue({
      status: 'granted',
    });
    (Notifications.getDevicePushTokenAsync as jest.Mock).mockResolvedValue({
      data: 'fcm-mock-token-999',
      type: Platform.OS,
    });
    (registerDeviceToken as jest.Mock).mockResolvedValue(undefined);

    const token = await registerPushTokenWithBackend();

    expect(token).toBe('fcm-mock-token-999');
    expect(getCachedDeviceToken()).toBe('fcm-mock-token-999');
    expect(registerDeviceToken).toHaveBeenCalledWith({
      deviceName: 'Galaxy S21',
      platform: Platform.OS === 'ios' ? 'IOS' : 'ANDROID',
      token: 'fcm-mock-token-999',
    });
    expect(consoleLogSpy).toHaveBeenCalledWith(
      '[Push] Token başarıyla kaydedildi:',
      '***en-999',
    );
    expect(consoleLogSpy).not.toHaveBeenCalledWith(
      expect.anything(),
      'fcm-mock-token-999',
    );
    consoleLogSpy.mockRestore();
  });

  it('token yenilendiğinde yeni tokenı backend e kaydeder', async () => {
    let tokenListener: (token: Notifications.DevicePushToken) => void =
      jest.fn();
    (Notifications.addPushTokenListener as jest.Mock).mockImplementation(
      (listener) => {
        tokenListener = listener;
        return { remove: jest.fn() };
      },
    );
    (registerDeviceToken as jest.Mock).mockResolvedValue(undefined);
    setCachedDeviceToken('old-fcm-token');

    subscribeToPushTokenChanges();
    tokenListener({ data: 'renewed-fcm-token', type: Platform.OS } as never);
    await Promise.resolve();

    expect(registerDeviceToken).toHaveBeenCalledWith(
      expect.objectContaining({ token: 'renewed-fcm-token' }),
    );
    expect(getCachedDeviceToken()).toBe('renewed-fcm-token');
  });

  it('aynı token için gereksiz renewal kaydı yapmaz', () => {
    let tokenListener: (token: Notifications.DevicePushToken) => void =
      jest.fn();
    (Notifications.addPushTokenListener as jest.Mock).mockImplementation(
      (listener) => {
        tokenListener = listener;
        return { remove: jest.fn() };
      },
    );
    setCachedDeviceToken('same-fcm-token');

    subscribeToPushTokenChanges();
    tokenListener({ data: 'same-fcm-token', type: Platform.OS } as never);

    expect(registerDeviceToken).not.toHaveBeenCalled();
  });

  it('token renewal listenerını cleanup sırasında kaldırır', () => {
    const remove = jest.fn();
    (Notifications.addPushTokenListener as jest.Mock).mockReturnValue({
      remove,
    });

    const unsubscribe = subscribeToPushTokenChanges();
    unsubscribe();

    expect(remove).toHaveBeenCalledTimes(1);
  });

  it('foreground bildirim davranışını korur', async () => {
    expect(mockForegroundHandler).toBeDefined();

    await expect(mockForegroundHandler?.handleNotification()).resolves.toEqual({
      shouldPlaySound: true,
      shouldSetBadge: true,
      shouldShowBanner: true,
      shouldShowList: true,
    });
  });

  it('warm/background tap için doğru recordId ile yönlendirir', () => {
    let responseListener: (
      response: Notifications.NotificationResponse,
    ) => void = jest.fn();
    (Notifications.addNotificationResponseReceivedListener as jest.Mock).mockImplementation(
      (listener) => {
        responseListener = listener;
        return { remove: jest.fn() };
      },
    );
    const onNavigate = jest.fn();

    subscribeToNotificationResponses(onNavigate);
    responseListener(notificationResponse(validRecordId));

    expect(onNavigate).toHaveBeenCalledWith(validRecordId);
    expect(Notifications.clearLastNotificationResponse).toHaveBeenCalledTimes(1);
  });

  it('cold-start response için doğru recordId ile yönlendirir', () => {
    (Notifications.getLastNotificationResponse as jest.Mock).mockReturnValue(
      notificationResponse(validRecordId, 'cold-start-notification'),
    );
    const onNavigate = jest.fn();

    subscribeToNotificationResponses(onNavigate);

    expect(onNavigate).toHaveBeenCalledTimes(1);
    expect(onNavigate).toHaveBeenCalledWith(validRecordId);
    expect(Notifications.clearLastNotificationResponse).toHaveBeenCalledTimes(1);
  });

  it.each([undefined, '', 'not-a-uuid'])(
    'geçersiz payload (%p) için yönlendirme yapmaz',
    (recordId) => {
      (Notifications.getLastNotificationResponse as jest.Mock).mockReturnValue(
        notificationResponse(recordId, `invalid-${String(recordId)}`),
      );
      const onNavigate = jest.fn();

      expect(() => subscribeToNotificationResponses(onNavigate)).not.toThrow();

      expect(onNavigate).not.toHaveBeenCalled();
    },
  );

  it('aynı response cold-start ve listener yollarında iki kez yönlendirme üretmez', () => {
    const response = notificationResponse(
      validRecordId,
      'duplicate-notification',
    );
    let responseListener: (
      response: Notifications.NotificationResponse,
    ) => void = jest.fn();
    (Notifications.getLastNotificationResponse as jest.Mock).mockReturnValue(
      response,
    );
    (Notifications.addNotificationResponseReceivedListener as jest.Mock).mockImplementation(
      (listener) => {
        responseListener = listener;
        return { remove: jest.fn() };
      },
    );
    const onNavigate = jest.fn();

    subscribeToNotificationResponses(onNavigate);
    responseListener(response);

    expect(onNavigate).toHaveBeenCalledTimes(1);
  });
});
