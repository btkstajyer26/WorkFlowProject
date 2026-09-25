import { getCurrentUser } from './users';

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

describe('users API', () => {
  const fetchMock = jest.fn();

  beforeEach(() => {
    fetchMock.mockReset();
    globalThis.fetch = fetchMock as typeof fetch;
  });

  it('yeniden adlandırılmış yerleşik rolü systemKey ve gösterim adıyla kabul eder', async () => {
    fetchMock.mockResolvedValue(
      jsonResponse(200, {
        active: true,
        createdAt: '2026-08-27T10:00:00Z',
        email: 'ahmet@example.com',
        firstName: 'Ahmet',
        id: 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
        lastName: 'Yılmaz',
        permissionCodes: ['RECORD_CREATE', 'RECORD_EDIT'],
        roleId: 1,
        roleName: 'Uzman Personel',
        systemKey: 'CALISAN',
      }),
    );

    const result = await getCurrentUser();

    expect(result.roleId).toBe(1);
    expect(result.systemKey).toBe('CALISAN');
    expect(result.roleName).toBe('Uzman Personel');
    expect(result.permissionCodes).toEqual(['RECORD_CREATE', 'RECORD_EDIT']);
  });

  it('dinamik rolü systemKey olmadan kabul eder', async () => {
    fetchMock.mockResolvedValue(
      jsonResponse(200, {
        active: true,
        createdAt: '2026-08-27T10:00:00Z',
        email: 'ayse@example.com',
        firstName: 'Ayşe',
        id: 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a22',
        lastName: 'Yılmaz',
        permissionCodes: ['RECORD_CREATE'],
        roleId: 9,
        roleName: 'Hukuk Uzmanı',
        systemKey: null,
      }),
    );

    const result = await getCurrentUser();

    expect(result.roleId).toBe(9);
    expect(result.systemKey).toBeNull();
    expect(result.roleName).toBe('Hukuk Uzmanı');
    expect(result.permissionCodes).toEqual(['RECORD_CREATE']);
  });
});
