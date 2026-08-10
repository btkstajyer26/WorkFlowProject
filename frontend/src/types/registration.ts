export type RegistrationRequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

export type RegistrationRequest = {
  id: string
  firstName: string
  lastName: string
  email: string
  requestedRole: 'CALISAN'
  status: RegistrationRequestStatus
  createdAt: string
}

export type CreateRegistrationRequestInput = {
  firstName: string
  lastName: string
  email: string
  password: string
}
