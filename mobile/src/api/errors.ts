export type ApiFieldError = {
  field: string;
  message: string;
};

export type ApiErrorBody = {
  code: string;
  fieldErrors?: ApiFieldError[];
  message: string;
  status: number;
  timestamp: string;
};

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null;
}

export function isApiErrorBody(value: unknown): value is ApiErrorBody {
  if (!isRecord(value)) return false;

  return (
    typeof value.code === 'string' &&
    typeof value.message === 'string' &&
    typeof value.status === 'number' &&
    typeof value.timestamp === 'string'
  );
}

type ApiClientErrorOptions = {
  cause?: unknown;
  code: string;
  fieldErrors?: ApiFieldError[];
  message: string;
  status: number;
};

export class ApiClientError extends Error {
  readonly code: string;
  readonly fieldErrors: ApiFieldError[];
  readonly status: number;

  constructor({ cause, code, fieldErrors = [], message, status }: ApiClientErrorOptions) {
    super(message, { cause });
    this.name = 'ApiClientError';
    this.code = code;
    this.fieldErrors = fieldErrors;
    this.status = status;
  }
}

export function createHttpError(status: number, responseBody: unknown): ApiClientError {
  if (isApiErrorBody(responseBody)) {
    return new ApiClientError({
      code: responseBody.code,
      fieldErrors: responseBody.fieldErrors,
      message: responseBody.message,
      status: responseBody.status,
    });
  }

  return new ApiClientError({
    code: 'HTTP_ERROR',
    message: 'Sunucu isteği tamamlanamadı.',
    status,
  });
}

export function createNetworkError(error: unknown, timedOut: boolean): ApiClientError {
  return new ApiClientError({
    cause: error,
    code: timedOut ? 'REQUEST_TIMEOUT' : 'NETWORK_ERROR',
    message: timedOut
      ? 'Sunucu yanıt vermedi. Lütfen tekrar deneyin.'
      : 'Sunucuya ulaşılamadı. Ağ bağlantınızı kontrol edin.',
    status: 0,
  });
}
