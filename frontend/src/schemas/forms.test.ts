import { describe, expect, it } from 'vitest'
import { createUserSchema } from './admin'
import { changePasswordSchema, loginSchema } from './auth'
import { recordFormSchema } from './record'

describe('login schema', () => {
  it('geçersiz e-posta ve kısa şifreyi reddeder', () => {
    const result = loginSchema.safeParse({ email: 'yanlis', password: '123' })
    expect(result.success).toBe(false)
    if (!result.success) expect(result.error.issues.map((issue) => issue.path[0])).toEqual(['email', 'password'])
  })

  it('kurumsal demo girişini kabul eder', () => {
    expect(loginSchema.safeParse({ email: 'john.doe@kurum.gov.tr', password: 'demo123' }).success).toBe(true)
  })
})

describe('admin kullanıcı oluşturma şeması', () => {
  it('kullanıcı bilgilerini kırparak geçerli isteği kabul eder', () => {
    const result = createUserSchema.parse({
      firstName: '  Deniz ',
      lastName: ' Yılmaz  ',
      email: ' deniz.yilmaz@kurum.gov.tr ',
      password: 'demo123',
    })

    expect(result.firstName).toBe('Deniz')
    expect(result.lastName).toBe('Yılmaz')
    expect(result.email).toBe('deniz.yilmaz@kurum.gov.tr')
  })

  it('altı karakterden kısa şifreyi reddeder', () => {
    const result = createUserSchema.safeParse({
      firstName: 'Deniz',
      lastName: 'Yılmaz',
      email: 'deniz.yilmaz@kurum.gov.tr',
      password: '12345',
    })

    expect(result.success).toBe(false)
    if (!result.success) expect(result.error.issues.map((issue) => issue.path[0])).toContain('password')
  })
})

describe('şifre değiştirme şeması', () => {
  it('harf ve rakam içermeyen veya eşleşmeyen yeni şifreyi reddeder', () => {
    const result = changePasswordSchema.safeParse({
      currentPassword: 'Gecici123',
      newPassword: 'yalnizharf',
      newPasswordConfirm: 'farkli123',
    })

    expect(result.success).toBe(false)
    if (!result.success) {
      expect(result.error.issues.map((issue) => issue.path[0])).toEqual(['newPassword', 'newPasswordConfirm'])
    }
  })

  it('geçerli ve eşleşen yeni şifreyi kabul eder', () => {
    expect(changePasswordSchema.safeParse({
      currentPassword: 'Gecici123',
      newPassword: 'YeniParola123',
      newPasswordConfirm: 'YeniParola123',
    }).success).toBe(true)
  })

  it('yeni şifre mevcut şifreyle aynıysa reddeder', () => {
    const result = changePasswordSchema.safeParse({
      currentPassword: 'Gecici123',
      newPassword: 'Gecici123',
      newPasswordConfirm: 'Gecici123',
    })

    expect(result.success).toBe(false)
    if (!result.success) {
      expect(result.error.issues).toContainEqual(expect.objectContaining({
        path: ['newPassword'],
        message: 'Yeni şifreniz mevcut şifrenizle aynı olamaz.',
      }))
    }
  })
})

describe('record form schema', () => {
  it('boş zorunlu alanların tamamını raporlar', () => {
    const result = recordFormSchema.safeParse({ title: '', categoryId: 0, description: '' })
    expect(result.success).toBe(false)
    if (!result.success) expect(result.error.issues.map((issue) => issue.path[0])).toEqual(['title', 'categoryId', 'description'])
  })

  it('seçilmemiş kategori kimliğini reddeder', () => {
    const result = recordFormSchema.safeParse({ title: 'Talep', categoryId: 0, description: 'Açıklama' })
    expect(result.success).toBe(false)
  })

  it('geçerli kayıt verisini kırpılmış haliyle döndürür', () => {
    const result = recordFormSchema.parse({ title: '  Talep  ', categoryId: 1, description: '  Açıklama  ' })
    expect(result).toEqual({ title: 'Talep', categoryId: 1, description: 'Açıklama' })
  })
})
