import {
  getNotifications,
  getUnreadNotificationCount,
  getUnreadNotifications,
  markNotificationAsRead,
  type NotificationItem,
} from './notifications';

function jsonResponse(status: number, body: unknown): Response {
  return {
    headers: {
      get: () => 'application/json',
    },
    ok: status >= 200 && status < 300,
    status,
    text: jest.fn().mockResolvedValue(JSON.stringify(body)),
  } as unknown as Response;
}

describe('notifications API', () => {
  const fetchMock = jest.fn();
  const mockNotification: NotificationItem = {
    createdAt: '2026-08-27T10:00:00Z',
    id: 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    message: 'Evrak incelemenize sunuldu',
    notificationType: 'RECORD_SUBMITTED',
    read: false,
    recordId: 'd3b07384-d113-4632-8fe2-51a6597a7a58',
  };

  beforeEach(() => {
    fetchMock.mockReset();
    globalThis.fetch = fetchMock as typeof fetch;
  });

  it('getNotifications: sayfalanmış bildirim listesini döner', async () => {
    fetchMock.mockResolvedValue(
      jsonResponse(200, {
        content: [mockNotification],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
      }),
    );

    const result = await getNotifications({ page: 0, size: 20 });
    expect(result.content).toHaveLength(1);
    expect(result.content[0].id).toBe(mockNotification.id);
    expect(result.totalElements).toBe(1);
  });

  it('getUnreadNotifications: okunmamış bildirim listesini döner', async () => {
    fetchMock.mockResolvedValue(jsonResponse(200, [mockNotification]));

    const result = await getUnreadNotifications();
    expect(result).toHaveLength(1);
    expect(result[0].read).toBe(false);
  });

  it('getUnreadNotificationCount: okunmamış sayısını sayı olarak döner', async () => {
    fetchMock.mockResolvedValue(jsonResponse(200, 5));

    const count = await getUnreadNotificationCount();
    expect(count).toBe(5);
  });

  it('markNotificationAsRead: PUT isteği gönderir', async () => {
    fetchMock.mockResolvedValue({
      headers: { get: () => 'application/json' },
      ok: true,
      status: 204,
      text: jest.fn().mockResolvedValue(''),
    } as unknown as Response);

    await markNotificationAsRead('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11');
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining(
        '/api/notifications/a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11/read',
      ),
      expect.objectContaining({ method: 'PUT' }),
    );
  });
});
