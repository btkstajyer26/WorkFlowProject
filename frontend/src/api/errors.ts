import { isAxiosError } from 'axios'

export type ApiFieldError = {
  field: string
  message: string
}

export type ApiErrorBody = {
  code: string
  message: string
  status: number
  timestamp: string
  fieldErrors?: ApiFieldError[]
}

function isApiErrorBody(value: unknown): value is ApiErrorBody {
  if (!value || typeof value !== 'object') return false
  const candidate = value as Partial<ApiErrorBody>
  return (
    typeof candidate.code === 'string' &&
    typeof candidate.message === 'string' &&
    typeof candidate.status === 'number' &&
    typeof candidate.timestamp === 'string'
  )
}

export class ApiClientError extends Error {
  status: number
  code: string
  fieldErrors: ApiFieldError[]

  constructor(body: Pick<ApiErrorBody, 'code' | 'message' | 'status'> & Partial<ApiErrorBody>) {
    super(body.message)
    this.name = 'ApiClientError'
    this.status = body.status
    this.code = body.code
    this.fieldErrors = body.fieldErrors ?? []
  }
}

export function sanitizeErrorMessage(message: string, code?: string, status?: number): string {
  const normalized = message.trim()

  if (
    code === 'METHOD_NOT_ALLOWED' ||
    status === 405 ||
    /^Request method '\w+' (is )?not supported/i.test(normalized)
  ) {
    return 'İstenen işlem bu kaynak için geçerli değil veya desteklenmiyor.'
  }

  if (code === 'UNSUPPORTED_MEDIA_TYPE' || status === 415) {
    return 'Desteklenmeyen dosya veya içerik türü.'
  }

  if (
    code === 'INTERNAL_ERROR' ||
    status === 500 ||
    /Exception|NullPointer|ServletException|HttpMediaType/i.test(normalized)
  ) {
    return 'Sunucu tarafında bir hata oluştu. Lütfen tekrar deneyin.'
  }

  return normalized || 'Beklenmeyen bir hata oluştu.'
}

export function toApiClientError(error: unknown) {
  if (error instanceof ApiClientError) return error

  if (isAxiosError(error)) {
    const responseBody: unknown = error.response?.data
    if (isApiErrorBody(responseBody)) {
      return new ApiClientError({
        ...responseBody,
        message: sanitizeErrorMessage(responseBody.message, responseBody.code, responseBody.status),
      })
    }

    const status = error.response?.status ?? 0
    return new ApiClientError({
      code: error.response ? (status === 405 ? 'METHOD_NOT_ALLOWED' : 'HTTP_ERROR') : 'NETWORK_ERROR',
      message: error.response
        ? sanitizeErrorMessage('Sunucu isteği tamamlanamadı.', undefined, status)
        : 'Sunucuya ulaşılamadı.',
      status,
    })
  }

  return new ApiClientError({
    code: 'UNKNOWN_ERROR',
    message: error instanceof Error ? sanitizeErrorMessage(error.message) : 'Beklenmeyen bir hata oluştu.',
    status: 0,
  })
}
