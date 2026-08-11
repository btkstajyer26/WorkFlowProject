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

export function toApiClientError(error: unknown) {
  if (error instanceof ApiClientError) return error

  if (isAxiosError(error)) {
    const responseBody: unknown = error.response?.data
    if (isApiErrorBody(responseBody)) return new ApiClientError(responseBody)

    return new ApiClientError({
      code: error.response ? 'HTTP_ERROR' : 'NETWORK_ERROR',
      message: error.response
        ? 'Sunucu isteği tamamlanamadı.'
        : 'Sunucuya ulaşılamadı.',
      status: error.response?.status ?? 0,
    })
  }

  return new ApiClientError({
    code: 'UNKNOWN_ERROR',
    message: error instanceof Error ? error.message : 'Beklenmeyen bir hata oluştu.',
    status: 0,
  })
}
