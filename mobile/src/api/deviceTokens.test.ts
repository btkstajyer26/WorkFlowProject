import {
  deleteDeviceToken,
  registerDeviceToken,
  type DeviceTokenRequest,
} from './deviceTokens';

describe('deviceTokens API', () => {
  const fetchMock = jest.fn();

  beforeEach(() => {
    fetchMock.mockReset();
    globalThis.fetch = fetchMock as typeof fetch;
  });

  it('registerDeviceToken: POST isteği ile token bilgilerini gönderir', async () => {
    fetchMock.mockResolvedValue({
      headers: { get: () => 'application/json' },
      ok: true,
      status: 200,
      text: jest.fn().mockResolvedValue(''),
    } as unknown as Response);

    const request: DeviceTokenRequest = {
      deviceName: 'Samsung Galaxy A34',
      platform: 'ANDROID',
      token: 'fcm-sample-token-123',
    };

    await registerDeviceToken(request);

    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/api/device-tokens'),
      expect.objectContaining({
        body: JSON.stringify(request),
        method: 'POST',
      }),
    );
  });

  it('deleteDeviceToken: DELETE isteği ile tokenı siler', async () => {
    fetchMock.mockResolvedValue({
      headers: { get: () => 'application/json' },
      ok: true,
      status: 204,
      text: jest.fn().mockResolvedValue(''),
    } as unknown as Response);

    await deleteDeviceToken('fcm-sample-token-123');

    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/api/device-tokens'),
      expect.objectContaining({
        body: JSON.stringify({ token: 'fcm-sample-token-123' }),
        method: 'DELETE',
      }),
    );
  });
});
