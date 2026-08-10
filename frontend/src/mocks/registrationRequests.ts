import { demoAccounts } from './users'
import type {
  CreateRegistrationRequestInput,
  RegistrationRequest,
} from '../types/registration'

const mockRegistrationRequestKey = 'ebys:mock-registration-requests:v1'
const registrationRequestStatuses = ['PENDING', 'APPROVED', 'REJECTED'] as const

function isRegistrationRequest(value: unknown): value is RegistrationRequest {
  if (!value || typeof value !== 'object') return false
  const candidate = value as Partial<RegistrationRequest>
  return (
    typeof candidate.id === 'string' &&
    typeof candidate.firstName === 'string' &&
    typeof candidate.lastName === 'string' &&
    typeof candidate.email === 'string' &&
    candidate.requestedRole === 'CALISAN' &&
    Boolean(candidate.status && registrationRequestStatuses.includes(candidate.status)) &&
    typeof candidate.createdAt === 'string'
  )
}

function persistMockRegistrationRequests(requests: RegistrationRequest[]) {
  window.localStorage.setItem(mockRegistrationRequestKey, JSON.stringify(requests))
}

export function readMockRegistrationRequests(): RegistrationRequest[] {
  try {
    const stored = window.localStorage.getItem(mockRegistrationRequestKey)
    if (!stored) return []
    const parsed: unknown = JSON.parse(stored)
    if (!Array.isArray(parsed)) return []
    return parsed.filter(isRegistrationRequest)
  } catch {
    return []
  }
}

export function createMockRegistrationRequest(
  input: CreateRegistrationRequestInput,
): RegistrationRequest {
  const email = input.email.trim().toLowerCase()
  const currentRequests = readMockRegistrationRequests()
  const emailAlreadyExists = demoAccounts.some((account) => account.email === email)
    || currentRequests.some((request) => request.email === email && request.status !== 'REJECTED')

  if (emailAlreadyExists) {
    throw new Error('Bu e-posta adresiyle kayıtlı bir hesap veya bekleyen talep zaten var.')
  }

  const request: RegistrationRequest = {
    id: `registration-${crypto.randomUUID()}`,
    firstName: input.firstName.trim(),
    lastName: input.lastName.trim(),
    email,
    requestedRole: 'CALISAN',
    status: 'PENDING',
    createdAt: new Date().toISOString(),
  }

  persistMockRegistrationRequests([request, ...currentRequests])

  return request
}

export function updateMockRegistrationRequestStatus(
  requestId: string,
  status: Exclude<RegistrationRequest['status'], 'PENDING'>,
) {
  const currentRequests = readMockRegistrationRequests()
  if (!currentRequests.some((request) => request.id === requestId)) {
    throw new Error('Kayıt talebi bulunamadı.')
  }

  const updatedRequests = currentRequests.map((request) => request.id === requestId
    ? { ...request, status }
    : request)
  persistMockRegistrationRequests(updatedRequests)
}
