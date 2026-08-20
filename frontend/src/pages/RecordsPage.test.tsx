import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { MemoryRouter, useLocation } from 'react-router'
import { describe, expect, it } from 'vitest'
import { CategoryProvider } from '../context/CategoryContext'
import { getDemoUserByRole } from '../mocks/users'
import { seedAuthenticatedUser } from '../test/auth'
import { apiBaseUrl } from '../api/config'
import { apiMockServer } from '../mocks/api/server'
import { AppQueryProvider } from '../query/queryClient'
import { RecordsPage } from './RecordsPage'

function LocationProbe() {
  const location = useLocation()
  return <output data-testid="location-search">{location.search}</output>
}

async function renderEmployeeRecords(initialEntry: string) {
  await seedAuthenticatedUser('CALISAN')
  const employee = getDemoUserByRole('CALISAN')
  return render(
    <MemoryRouter initialEntries={[initialEntry]}>
      <AppQueryProvider>
        <CategoryProvider>
          <RecordsPage role={employee.role} />
          <LocationProbe />
        </CategoryProvider>
      </AppQueryProvider>
    </MemoryRouter>,
  )
}

describe('RecordsPage filters', () => {
  it('tek durumlu görünümde çelişen durum parametresini URL’den temizler', async () => {
    await renderEmployeeRecords('/kayitlar?gorunum=taslaklar&durum=REDDEDILDI')

    expect(screen.getByRole('combobox', { name: 'Durum' })).toBeDisabled()
    expect(screen.getByRole('combobox', { name: 'Durum' })).toHaveValue('TASLAK')
    await waitFor(() => expect(screen.getByTestId('location-search')).toHaveTextContent('?gorunum=taslaklar'))
    expect(screen.getByTestId('location-search')).not.toHaveTextContent('durum=')
  })

  it('kullanıcı filtresini URL sorgusuna yazar', async () => {
    const user = userEvent.setup()
    await renderEmployeeRecords('/kayitlar')

    const categorySelect = screen.getByRole('combobox', { name: 'Kategori' })
    await waitFor(() => expect(categorySelect).toBeEnabled())
    await user.selectOptions(categorySelect, '4')
    expect(screen.getByTestId('location-search')).toHaveTextContent('kategori=4')
  })

  it('aramayı debounce sonrasında URL sorgusuna yazar', async () => {
    const user = userEvent.setup()
    await renderEmployeeRecords('/kayitlar')

    await user.type(screen.getByRole('searchbox', { name: 'Başlık veya içerikle ara' }), 'sunucu')
    expect(screen.getByTestId('location-search')).not.toHaveTextContent('q=')
    await waitFor(() => expect(screen.getByTestId('location-search')).toHaveTextContent('q=sunucu'), { timeout: 1000 })
  })

  it('oluşturan kişi filtresini debounce sonrasında URL sorgusuna yazar', async () => {
    const user = userEvent.setup()
    await renderEmployeeRecords('/kayitlar')

    await user.click(screen.getByRole('button', { name: 'Filtreler' }))
    await user.type(screen.getByRole('searchbox', { name: 'Oluşturan kişi' }), 'John Doe')
    expect(screen.getByTestId('location-search')).not.toHaveTextContent('olusturan=')
    await waitFor(() => expect(screen.getByTestId('location-search')).toHaveTextContent('olusturan=John+Doe'), { timeout: 1000 })
  })

  it('yalnız backend arama sözleşmesinde bulunan liste alanlarını gösterir', async () => {
    await renderEmployeeRecords('/kayitlar')

    expect(screen.getByPlaceholderText('Başlık veya içerikte ara...')).toBeInTheDocument()
    expect(screen.queryByRole('columnheader', { name: 'Son işlem' })).not.toBeInTheDocument()
    expect(await screen.findByRole('columnheader', { name: 'Oluşturulma' })).toBeInTheDocument()
    expect(screen.getByRole('columnheader', { name: 'Oluşturan' })).toBeInTheDocument()
    expect(screen.queryByText('EBYS-2026-000023')).not.toBeInTheDocument()
  })

  it('geçersiz filtreleri temizler ve taşan sayfayı son geçerli sayfaya çeker', async () => {
    await renderEmployeeRecords('/kayitlar?gorunum=bilinmeyen&kategori=Yok&durum=BOZUK&baslangic=2026-99-99&bitis=x&sayfa=999&boyut=7')

    await waitFor(() => expect(screen.getByTestId('location-search')).toHaveTextContent(/^$/))
    expect(screen.getByTestId('location-search')).not.toHaveTextContent('gorunum=')
    expect(screen.getByTestId('location-search')).not.toHaveTextContent('kategori=')
    expect(screen.getByTestId('location-search')).not.toHaveTextContent('durum=')
  })

  it('başlangıç tarihi bitiş tarihinden sonraysa açık doğrulama hatası gösterir', async () => {
    await renderEmployeeRecords('/kayitlar?baslangic=2026-08-05&bitis=2026-08-01')

    expect(screen.getByRole('alert')).toHaveTextContent('Oluşturulma başlangıcı bitiş tarihinden sonra olamaz')
  })

  it('kategori isteği başarısız olursa tekrar deneyerek listeyi yükler', async () => {
    const user = userEvent.setup()
    let requestCount = 0
    apiMockServer.use(
      http.get(`${apiBaseUrl}/api/categories`, () => {
        requestCount += 1
        return HttpResponse.json({ message: 'Geçici hata' }, { status: 500 })
      }),
    )

    await renderEmployeeRecords('/kayitlar')
    await user.click(screen.getByRole('button', { name: 'Filtreler' }))
    await waitFor(() => expect(requestCount).toBeGreaterThan(0))

    expect((await screen.findAllByText(/Kategoriler yüklenemedi/)).length).toBeGreaterThan(0)
    apiMockServer.resetHandlers()
    await user.click(screen.getAllByRole('button', { name: 'Tekrar dene' })[0])

    const categorySelect = screen.getByRole('combobox', { name: 'Kategori' })
    await waitFor(() => expect(categorySelect).toBeEnabled())
    expect(screen.getByRole('option', { name: 'Mali' })).toBeInTheDocument()
  })
})
