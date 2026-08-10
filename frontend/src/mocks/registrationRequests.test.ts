import { describe, expect, it } from 'vitest'
import {
  createMockRegistrationRequest,
  readMockRegistrationRequests,
  updateMockRegistrationRequestStatus,
} from './registrationRequests'

describe('mock registration request store', () => {
  it('talebi Çalışan ve bekliyor durumunda saklar, ham şifreyi depolamaz', () => {
    const request = createMockRegistrationRequest({
      firstName: ' Deniz ',
      lastName: ' Yılmaz ',
      email: ' DENIZ.YILMAZ@kurum.gov.tr ',
      password: 'guvenli123',
    })

    expect(request).toMatchObject({
      firstName: 'Deniz',
      lastName: 'Yılmaz',
      email: 'deniz.yilmaz@kurum.gov.tr',
      requestedRole: 'CALISAN',
      status: 'PENDING',
    })
    expect(readMockRegistrationRequests()).toEqual([request])
    const storedKey = window.localStorage.key(0)
    expect(storedKey).not.toBeNull()
    expect(window.localStorage.getItem(storedKey ?? '')).not.toContain('guvenli123')
  })

  it('kayıtlı veya bekleyen e-posta için ikinci talebi reddeder', () => {
    expect(() => createMockRegistrationRequest({
      firstName: 'John',
      lastName: 'Doe',
      email: 'john.doe@kurum.gov.tr',
      password: 'guvenli123',
    })).toThrow('kayıtlı bir hesap veya bekleyen talep')

    createMockRegistrationRequest({
      firstName: 'Deniz',
      lastName: 'Yılmaz',
      email: 'deniz.yilmaz@kurum.gov.tr',
      password: 'guvenli123',
    })

    expect(() => createMockRegistrationRequest({
      firstName: 'Başka',
      lastName: 'Kullanıcı',
      email: 'deniz.yilmaz@kurum.gov.tr',
      password: 'baskasifre123',
    })).toThrow('kayıtlı bir hesap veya bekleyen talep')
  })

  it('admin kararını saklar ve reddedilen e-posta için yeniden başvuruya izin verir', () => {
    const request = createMockRegistrationRequest({
      firstName: 'Deniz',
      lastName: 'Yılmaz',
      email: 'deniz.yilmaz@kurum.gov.tr',
      password: 'guvenli123',
    })

    updateMockRegistrationRequestStatus(request.id, 'REJECTED')
    expect(readMockRegistrationRequests()[0]).toMatchObject({
      id: request.id,
      status: 'REJECTED',
    })

    expect(() => createMockRegistrationRequest({
      firstName: 'Deniz',
      lastName: 'Yılmaz',
      email: 'deniz.yilmaz@kurum.gov.tr',
      password: 'yenisifre123',
    })).not.toThrow()
  })
})
