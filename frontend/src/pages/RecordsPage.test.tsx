import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, useLocation } from 'react-router'
import { describe, expect, it } from 'vitest'
import { WorkflowProvider } from '../context/WorkflowContext'
import { getDemoUserByRole } from '../mocks/users'
import { RecordsPage } from './RecordsPage'

function LocationProbe() {
  const location = useLocation()
  return <output data-testid="location-search">{location.search}</output>
}

function renderEmployeeRecords(initialEntry: string) {
  const employee = getDemoUserByRole('CALISAN')
  return render(
    <MemoryRouter initialEntries={[initialEntry]}>
      <WorkflowProvider user={employee}>
        <RecordsPage role={employee.role} />
        <LocationProbe />
      </WorkflowProvider>
    </MemoryRouter>,
  )
}

describe('RecordsPage filters', () => {
  it('tek durumlu görünümde çelişen durum parametresini URL’den temizler', async () => {
    renderEmployeeRecords('/kayitlar?gorunum=taslaklar&durum=REDDEDILDI')

    expect(screen.getByRole('combobox', { name: 'Durum' })).toBeDisabled()
    expect(screen.getByRole('combobox', { name: 'Durum' })).toHaveValue('TASLAK')
    await waitFor(() => expect(screen.getByTestId('location-search')).toHaveTextContent('?gorunum=taslaklar'))
    expect(screen.getByTestId('location-search')).not.toHaveTextContent('durum=')
  })

  it('kullanıcı filtresini URL sorgusuna yazar', async () => {
    const user = userEvent.setup()
    renderEmployeeRecords('/kayitlar')

    await user.selectOptions(screen.getByRole('combobox', { name: 'Kategori' }), 'Bilgi İşlem')
    expect(screen.getByTestId('location-search')).toHaveTextContent('kategori=Bilgi')
  })

  it('aramayı debounce sonrasında URL sorgusuna yazar', async () => {
    const user = userEvent.setup()
    renderEmployeeRecords('/kayitlar')

    await user.type(screen.getByRole('searchbox'), 'sunucu')
    expect(screen.getByTestId('location-search')).not.toHaveTextContent('q=')
    await waitFor(() => expect(screen.getByTestId('location-search')).toHaveTextContent('q=sunucu'), { timeout: 1000 })
  })

  it('geçersiz filtreleri temizler ve taşan sayfayı son geçerli sayfaya çeker', async () => {
    renderEmployeeRecords('/kayitlar?gorunum=bilinmeyen&kategori=Yok&durum=BOZUK&baslangic=2026-99-99&bitis=x&sayfa=999&boyut=7')

    await waitFor(() => expect(screen.getByTestId('location-search')).toHaveTextContent('?sayfa=2'))
    expect(screen.getByTestId('location-search')).not.toHaveTextContent('gorunum=')
    expect(screen.getByTestId('location-search')).not.toHaveTextContent('kategori=')
    expect(screen.getByTestId('location-search')).not.toHaveTextContent('durum=')
  })

  it('başlangıç tarihi bitiş tarihinden sonraysa açık doğrulama hatası gösterir', () => {
    renderEmployeeRecords('/kayitlar?baslangic=2026-08-05&bitis=2026-08-01')

    expect(screen.getByRole('alert')).toHaveTextContent('Başlangıç tarihi bitiş tarihinden sonra olamaz')
  })
})
