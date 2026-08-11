import { describe, expect, it } from 'vitest'
import { loginSchema, registrationSchema } from './auth'
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

describe('registration schema', () => {
  it('eşleşmeyen şifreleri reddeder', () => {
    const result = registrationSchema.safeParse({
      firstName: 'Deniz',
      lastName: 'Yılmaz',
      email: 'deniz.yilmaz@kurum.gov.tr',
      password: 'guvenli123',
      confirmPassword: 'farkli123',
    })

    expect(result.success).toBe(false)
    if (!result.success) {
      expect(result.error.issues.map((issue) => issue.path[0])).toContain('confirmPassword')
    }
  })

  it('kullanıcı bilgilerini kırparak geçerli kayıt talebini kabul eder', () => {
    const result = registrationSchema.parse({
      firstName: '  Deniz ',
      lastName: ' Yılmaz  ',
      email: ' deniz.yilmaz@kurum.gov.tr ',
      password: 'guvenli123',
      confirmPassword: 'guvenli123',
    })

    expect(result.firstName).toBe('Deniz')
    expect(result.lastName).toBe('Yılmaz')
    expect(result.email).toBe('deniz.yilmaz@kurum.gov.tr')
  })
})

describe('record form schema', () => {
  it('boş zorunlu alanların tamamını raporlar', () => {
    const result = recordFormSchema.safeParse({ title: '', category: '', description: '' })
    expect(result.success).toBe(false)
    if (!result.success) expect(result.error.issues.map((issue) => issue.path[0])).toEqual(['title', 'category', 'description'])
  })

  it('yalnızca boşluktan oluşan kategori değerini reddeder', () => {
    const result = recordFormSchema.safeParse({ title: 'Talep', category: '   ', description: 'Açıklama' })
    expect(result.success).toBe(false)
  })

  it('geçerli kayıt verisini kırpılmış haliyle döndürür', () => {
    const result = recordFormSchema.parse({ title: '  Talep  ', category: 'İdari', description: '  Açıklama  ' })
    expect(result).toEqual({ title: 'Talep', category: 'İdari', description: 'Açıklama' })
  })
})
