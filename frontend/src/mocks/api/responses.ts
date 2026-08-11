import { HttpResponse } from 'msw'
import type { ApiErrorBody, ApiFieldError } from '../../api/errors'

export function apiErrorResponse(
  status: number,
  code: string,
  message: string,
  fieldErrors?: ApiFieldError[],
) {
  return HttpResponse.json<ApiErrorBody>({
    code,
    message,
    status,
    timestamp: new Date().toISOString(),
    ...(fieldErrors?.length ? { fieldErrors } : {}),
  }, { status })
}

export function unauthorizedResponse() {
  return apiErrorResponse(401, 'UNAUTHORIZED', 'Kimlik doğrulaması gerekli')
}

export function forbiddenResponse(message = 'Bu işlem için yetkiniz yok') {
  return apiErrorResponse(403, 'FORBIDDEN', message)
}
