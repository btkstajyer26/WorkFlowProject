import { ApiClientError, createHttpError, createNetworkError } from './errors';

const DEFAULT_TIMEOUT_MS = 15_000;
const rawApiBaseUrl = process.env.EXPO_PUBLIC_API_BASE_URL;

if (!rawApiBaseUrl) {
  throw new Error('EXPO_PUBLIC_API_BASE_URL tanımlanmalıdır.');
}

const API_BASE_URL = rawApiBaseUrl.replace(/\/+$/, '');

type ApiRequestOptions = RequestInit & {
  accessToken?: string;
  json?: unknown;
  timeoutMs?: number;
};

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

async function apiRequest<T>(
  path: string,
  options: ApiRequestOptions = {},
): Promise<T> {
  const {
    accessToken,
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

    if (!response.ok) {
      throw createHttpError(response.status, responseBody);
    }

    return responseBody as T;
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

export { API_BASE_URL, apiRequest };
export type { ApiRequestOptions };
