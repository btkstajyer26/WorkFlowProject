import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { describe, expect, it } from 'vitest'
import App from '../App'
import { getDemoUserByRole } from '../mocks/users'

const mockSessionKey = 'ebys:mock-session:v1'

function renderEmployeeApp(path: string) {
  window.sessionStorage.setItem(mockSessionKey, JSON.stringify(getDemoUserByRole('CALISAN')))
  return render(
    <MemoryRouter initialEntries={[path]}>
      <App />
    </MemoryRouter>,
  )
}

describe('Kayıt formu edge-case davranışları', () => {
  it('kaydedilmemiş yeni kayıt formu kapatılırken onay ister ve odağı dialog içinde tutar', async () => {
    const user = userEvent.setup()
    renderEmployeeApp('/dashboard')

    await user.click(await screen.findByRole('button', { name: 'Yeni Kayıt' }))
    expect(screen.getByRole('heading', { name: /Hoş geldiniz/ })).toBeInTheDocument()

    await user.type(await screen.findByLabelText(/Başlık/), 'Yeni talep')
    await user.click(screen.getByRole('button', { name: 'Yeni kayıt formunu kapat' }))

    const dialog = screen.getByRole('dialog', { name: 'Kaydedilmemiş değişiklikler var' })
    expect(dialog).toBeInTheDocument()
    await waitFor(() => expect(within(dialog).getByRole('button', { name: 'Düzenlemeye Devam Et' })).toHaveFocus())
    await user.tab({ shift: true })
    expect(within(dialog).getByRole('button', { name: 'Değişiklikleri Sil' })).toHaveFocus()
  })

  it('düzenleme ekranında aynı dosyayı ikinci kez eklemez', async () => {
    const user = userEvent.setup()
    renderEmployeeApp('/kayitlar/rec-002/duzenle')

    const fileInput = await screen.findByLabelText('Dosya ekle')
    const file = new File(['teklif'], 'teklif.pdf', { type: 'application/pdf', lastModified: 10 })
    await user.upload(fileInput, file)
    await user.upload(fileInput, file)

    expect(screen.getByRole('alert')).toHaveTextContent('Aynı dosya birden fazla kez eklenemez')
    expect(screen.getAllByText('teklif.pdf')).toHaveLength(1)
  })
})
