import {
  apiAuthenticatedOperation,
  apiRequest,
  clearApiAuthHandlers,
  setApiAuthHandlers,
} from './client';

function response(status: number, body?: unknown): Response {
  return {
    headers: {
      get: () => (body === undefined ? null : 'application/json'),
    },
    ok: status >= 200 && status < 300,
    status,
    text: jest.fn().mockResolvedValue(
      body === undefined ? '' : JSON.stringify(body),
    ),
  } as unknown as Response;
}

describe('apiClient auth davranışı', () => {
  const fetchMock = jest.fn();

  beforeEach(() => {
    clearApiAuthHandlers();
    fetchMock.mockReset();
    globalThis.fetch = fetchMock as typeof fetch;
  });

  afterAll(() => {
    clearApiAuthHandlers();
  });

  it('korumalı isteğe access token ekler', async () => {
    fetchMock.mockResolvedValue(response(200, { ok: true }));
    setApiAuthHandlers({
      getAccessToken: () => 'access-token',
      refreshAccessToken: jest.fn(),
    });

    await expect(apiRequest('/api/protected')).resolves.toEqual({ ok: true });

    const headers = fetchMock.mock.calls[0][1].headers as Headers;
    expect(headers.get('Authorization')).toBe('Bearer access-token');
  });

  it('401 sonrasında tokenı yenileyip isteği yalnız bir kez tekrarlar', async () => {
    const refreshAccessToken = jest.fn().mockResolvedValue('new-access-token');
    fetchMock
      .mockResolvedValueOnce(response(401, { message: 'expired' }))
      .mockResolvedValueOnce(response(200, { ok: true }));
    setApiAuthHandlers({
      getAccessToken: () => 'old-access-token',
      refreshAccessToken,
    });

    await expect(apiRequest('/api/protected')).resolves.toEqual({ ok: true });

    expect(refreshAccessToken).toHaveBeenCalledTimes(1);
    expect(fetchMock).toHaveBeenCalledTimes(2);
    const retryHeaders = fetchMock.mock.calls[1][1].headers as Headers;
    expect(retryHeaders.get('Authorization')).toBe('Bearer new-access-token');
  });

  it('dosya gibi harici işlemlerde de 401 sonrası yeni tokenla tekrarlar', async () => {
    const operation = jest
      .fn()
      .mockResolvedValueOnce({ status: 401 })
      .mockResolvedValueOnce({ status: 200, uri: 'file://download.pdf' });
    setApiAuthHandlers({
      getAccessToken: () => 'old-access-token',
      refreshAccessToken: jest.fn().mockResolvedValue('new-access-token'),
    });

    await expect(apiAuthenticatedOperation(operation)).resolves.toEqual({
      status: 200,
      uri: 'file://download.pdf',
    });
    expect(operation).toHaveBeenNthCalledWith(1, 'old-access-token');
    expect(operation).toHaveBeenNthCalledWith(2, 'new-access-token');
  });

  it('refresh başarısızsa harici işlemin ilk 401 sonucunu döndürür', async () => {
    const firstResult = { status: 401 };
    const operation = jest.fn().mockResolvedValue(firstResult);
    setApiAuthHandlers({
      getAccessToken: () => 'expired-token',
      refreshAccessToken: jest.fn().mockRejectedValue(new Error('refresh failed')),
    });

    await expect(apiAuthenticatedOperation(operation)).resolves.toBe(firstResult);
    expect(operation).toHaveBeenCalledTimes(1);
  });
});
