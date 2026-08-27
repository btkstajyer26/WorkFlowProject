import { z } from 'zod';

import { apiRequest } from './client';

export const devicePlatformSchema = z.enum(['ANDROID', 'IOS']);
export type DevicePlatform = z.infer<typeof devicePlatformSchema>;

export const deviceTokenRequestSchema = z.object({
  deviceName: z.string().nullish(),
  platform: devicePlatformSchema,
  token: z.string().min(1),
});

export type DeviceTokenRequest = z.infer<typeof deviceTokenRequestSchema>;

export async function registerDeviceToken(
  request: DeviceTokenRequest,
): Promise<void> {
  const parsed = deviceTokenRequestSchema.parse(request);
  await apiRequest<void>('/api/device-tokens', {
    json: {
      deviceName: parsed.deviceName || undefined,
      platform: parsed.platform,
      token: parsed.token,
    },
    method: 'POST',
  });
}

export async function deleteDeviceToken(token: string): Promise<void> {
  await apiRequest<void>('/api/device-tokens', {
    json: { token },
    method: 'DELETE',
  });
}
