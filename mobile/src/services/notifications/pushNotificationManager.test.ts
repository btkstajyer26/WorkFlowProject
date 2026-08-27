import * as Notifications from 'expo-notifications';

import { registerDeviceToken } from '@/api/deviceTokens';
import {
  getCachedDeviceToken,
  registerPushTokenWithBackend,
  setCachedDeviceToken,
  subscribeToNotificationResponses,
} from './pushNotificationManager';

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
  addNotificationReceivedListener: jest.fn().mockReturnValue({ remove: jest.fn() }),
  addNotificationResponseReceivedListener: jest.fn().mockReturnValue({ remove: jest.fn() }),
  getDevicePushTokenAsync: jest.fn(),
  getPermissionsAsync: jest.fn(),
  requestPermissionsAsync: jest.fn(),
  setNotificationHandler: jest.fn(),
}));

jest.mock('@/api/deviceTokens', () => ({
  registerDeviceToken: jest.fn(),
}));

describe('pushNotificationManager', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    setCachedDeviceToken(null);
    mockIsDevice = true;
  });

  it('fiziksel cihaz değilse token istemez ve null döner', async () => {
    mockIsDevice = false;

    const result = await registerPushTokenWithBackend();
    expect(result).toBeNull();
    expect(Notifications.getPermissionsAsync).not.toHaveBeenCalled();
  });

  it('izin verildiğinde tokenı alır ve backend e kaydeder', async () => {
    mockIsDevice = true;
    (Notifications.getPermissionsAsync as jest.Mock).mockResolvedValue({ status: 'granted' });
    (Notifications.getDevicePushTokenAsync as jest.Mock).mockResolvedValue({
      data: 'fcm-mock-token-999',
      type: 'fcm',
    });
    (registerDeviceToken as jest.Mock).mockResolvedValue(undefined);

    const token = await registerPushTokenWithBackend();
    expect(token).toBe('fcm-mock-token-999');
    expect(getCachedDeviceToken()).toBe('fcm-mock-token-999');
    expect(registerDeviceToken).toHaveBeenCalledWith(
      expect.objectContaining({
        token: 'fcm-mock-token-999',
      }),
    );
  });

  it('bildirime tıklandığında recordId ile yönlendirme callback ini tetikler', () => {
    let listenerCallback: ((response: unknown) => void) | undefined;
    (Notifications.addNotificationResponseReceivedListener as jest.Mock).mockImplementation(
      (cb) => {
        listenerCallback = cb;
        return { remove: jest.fn() };
      },
    );

    const onNavigateMock = jest.fn();
    const unsubscribe = subscribeToNotificationResponses(onNavigateMock);

    expect(listenerCallback).toBeDefined();

    // Simüle edilen bildirim tıklaması
    listenerCallback?.({
      notification: {
        request: {
          content: {
            data: {
              recordId: 'd3b07384-d113-4632-8fe2-51a6597a7a58',
            },
          },
        },
      },
    });

    expect(onNavigateMock).toHaveBeenCalledWith('d3b07384-d113-4632-8fe2-51a6597a7a58');

    unsubscribe();
  });
});
