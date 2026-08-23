import { ApiClientError, createHttpError, createNetworkError } from './errors';

const DEFAULT_TIMEOUT_MS = 15_000;
const rawApiBaseUrl = process.env.EXPO_PUBLIC_API_BASE_URL;

if (!rawApiBaseUrl) {
  throw new Error('EXPO_PUBLIC_API_BASE_URL tanımlanmalıdır.');
}

const API_BASE_URL = rawApiBaseUrl.replace(/\/+$/, '');

type ApiRequestOptions = RequestInit & {
  accessToken?: string;
  auth?: boolean;
  json?: unknown;
  timeoutMs?: number;
};

type ApiAuthHandlers = {
  getAccessToken: () => string | null;
  refreshAccessToken: () => Promise<string>;
};

type ApiFetchResult = {
  response: Response;
  responseBody: unknown;
};

type ApiAuthenticatedOperationResult = {
  status: number;
};

let apiAuthHandlers: ApiAuthHandlers | null = null;

function createRequestUrl(path: string): string {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`;
  return `${API_BASE_URL}${normalizedPath}`;
}

function createRequestHeaders(
  headers: HeadersInit | undefined,
  accessToken: string | undefined,
  hasJsonBody: boolean,
): Headers {
  const requestHeaders = new Headers(headers);
  requestHeaders.set('Accept', 'application/json');

  if (hasJsonBody) {
    requestHeaders.set('Content-Type', 'application/json');
  }

  if (accessToken) {
    requestHeaders.set('Authorization', `Bearer ${accessToken}`);
  }

  return requestHeaders;
}

async function readResponseBody(response: Response): Promise<unknown> {
  const responseText = await response.text();
  if (!responseText) return undefined;

  const contentType = response.headers.get('content-type') ?? '';
  if (!contentType.includes('application/json')) return responseText;

  try {
    return JSON.parse(responseText) as unknown;
  } catch {
    return responseText;
  }
}

async function executeFetch(
  path: string,
  options: ApiRequestOptions,
  accessToken: string | undefined,
): Promise<ApiFetchResult> {
  const {
    accessToken: _accessToken,
    auth: _auth,
    body,
    headers,
    json,
    signal,
    timeoutMs = DEFAULT_TIMEOUT_MS,
    ...requestOptions
  } = options;

  if (json !== undefined && body !== undefined) {
    throw new Error('Aynı istekte hem json hem body kullanılamaz.');
  }

  const abortController = new AbortController();
  let timedOut = false;
  const abortFromCaller = () => abortController.abort();

  if (signal?.aborted) abortController.abort();
  signal?.addEventListener('abort', abortFromCaller, { once: true });

  const timeoutId = setTimeout(() => {
    timedOut = true;
    abortController.abort();
  }, timeoutMs);

  try {
    const response = await fetch(createRequestUrl(path), {
      ...requestOptions,
      body: json === undefined ? body : JSON.stringify(json),
      headers: createRequestHeaders(headers, accessToken, json !== undefined),
      signal: abortController.signal,
    });
    const responseBody = await readResponseBody(response);
    return { response, responseBody };
  } catch (error) {
    if (error instanceof ApiClientError) {
      throw error;
    }

    if (signal?.aborted && error instanceof Error && error.name === 'AbortError') {
      throw error;
    }

    throw createNetworkError(error, timedOut);
  } finally {
    clearTimeout(timeoutId);
    signal?.removeEventListener('abort', abortFromCaller);
  }
}

async function executeApiRequest<T>(
  path: string,
  options: ApiRequestOptions,
  canRefresh: boolean,
): Promise<T> {
  const { accessToken: accessTokenOverride, auth = true } = options;
  const accessToken =
    accessTokenOverride ?? (auth ? (apiAuthHandlers?.getAccessToken() ?? undefined) : undefined);
  const { response, responseBody } = await executeFetch(path, options, accessToken);

  if (response.status === 401 && auth && canRefresh && apiAuthHandlers) {
    try {
      const refreshedAccessToken = await apiAuthHandlers.refreshAccessToken();
      return executeApiRequest<T>(
        path,
        { ...options, accessToken: refreshedAccessToken },
        false,
      );
    } catch {
      // Oturum yöneticisi başarısız refresh sonrasında yerel tokenları temizler.
      // Çağıran katmana asıl isteğin 401 cevabını iletiyoruz.
    }
  }

  if (!response.ok) {
    throw createHttpError(response.status, responseBody);
  }

  return responseBody as T;
}

function apiRequest<T>(path: string, options: ApiRequestOptions = {}): Promise<T> {
  return executeApiRequest<T>(path, options, true);
}

async function apiAuthenticatedOperation<T extends ApiAuthenticatedOperationResult>(
  operation: (accessToken: string | undefined) => Promise<T>,
): Promise<T> {
  const accessToken = apiAuthHandlers?.getAccessToken() ?? undefined;
  const result = await operation(accessToken);

  if (result.status !== 401 || !apiAuthHandlers) {
    return result;
  }

  try {
    const refreshedAccessToken = await apiAuthHandlers.refreshAccessToken();
    return await operation(refreshedAccessToken);
  } catch {
    // Oturum yöneticisi başarısız refresh sonrasında yerel tokenları temizler.
    // Çağıran katman ilk 401 sonucunu kendi işlem türüne göre ele alır.
    return result;
  }
}

function setApiAuthHandlers(handlers: ApiAuthHandlers): void {
  apiAuthHandlers = handlers;
}

function clearApiAuthHandlers(): void {
  apiAuthHandlers = null;
}

export {
  API_BASE_URL,
  apiAuthenticatedOperation,
  apiRequest,
  clearApiAuthHandlers,
  setApiAuthHandlers,
};
export type {
  ApiAuthenticatedOperationResult,
  ApiAuthHandlers,
  ApiRequestOptions,
};
