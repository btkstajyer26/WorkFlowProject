import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { getDemoUserByRole } from '../mocks/users'
import { ProfilePage } from './ProfilePage'

describe('ProfilePage', () => {
  it('yalnız mevcut kullanıcı bilgilerini ve rolünü gösterir', () => {
    render(<ProfilePage user={getDemoUserByRole('CALISAN')} />)

    expect(screen.getByRole('heading', { name: 'John Doe' })).toBeInTheDocument()
    expect(screen.getByText('john.doe@kurum.gov.tr')).toBeInTheDocument()
    expect(screen.getByText('Çalışan')).toBeInTheDocument()
    expect(screen.queryByText('Kurumsal kullanıcı')).not.toBeInTheDocument()
    expect(screen.queryByText('Hesap türü')).not.toBeInTheDocument()
    expect(screen.queryByText('Sistem rolü')).not.toBeInTheDocument()
    expect(screen.queryByText('Aktif hesap')).not.toBeInTheDocument()
  })
})
