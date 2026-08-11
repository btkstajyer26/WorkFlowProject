import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { describe, expect, it } from 'vitest'
import App from '../App'
import { seedAuthenticatedUser } from '../test/auth'

async function renderEmployeeApp(path: string) {
  await seedAuthenticatedUser('CALISAN')
  return render(
    <MemoryRouter initialEntries={[path]}>
      <App />
    </MemoryRouter>,
  )
}

describe('Kayıt formu edge-case davranışları', () => {
  it('kaydedilen taslaktan sonra yeni kayıt formunu boş açar', async () => {
    const user = userEvent.setup()
    await renderEmployeeApp('/dashboard')

    await user.click(await screen.findByRole('button', { name: 'Yeni Kayıt' }))
    await user.type(screen.getByLabelText(/Başlık/), 'Yeni talep')
    await user.selectOptions(screen.getByLabelText(/Kategori/), 'Bilgi İşlem')
    await user.type(screen.getByLabelText(/Açıklama/), 'Yeni kayıt açıklaması')
    await user.click(screen.getByRole('button', { name: 'Taslak Kaydet' }))
    await user.click(await screen.findByRole('button', { name: 'Yeni Kayıt Oluştur' }))

    expect(screen.getByLabelText(/Başlık/)).toHaveValue('')
    expect(screen.getByLabelText(/Kategori/)).toHaveValue('')
    expect(screen.getByLabelText(/Açıklama/)).toHaveValue('')
    expect(screen.getByRole('button', { name: 'Taslak Kaydet' })).toBeInTheDocument()
  })

  it('kaydedilen taslağı doğrudan incelemeye gönderir', async () => {
    const user = userEvent.setup()
    await renderEmployeeApp('/dashboard')

    await user.click(await screen.findByRole('button', { name: 'Yeni Kayıt' }))
    await user.type(screen.getByLabelText(/Başlık/), 'Doğrudan gönderilecek taslak')
    await user.selectOptions(screen.getByLabelText(/Kategori/), 'Bilgi İşlem')
    await user.type(screen.getByLabelText(/Açıklama/), 'Taslak kaydedildikten sonra incelemeye gönderilecek.')
    await user.click(screen.getByRole('button', { name: 'Taslak Kaydet' }))
    const submitButton = await screen.findByRole('button', { name: 'İncelemeye Gönder' })
    await waitFor(() => expect(submitButton).toBeEnabled())
    await user.click(submitButton)

    expect(await screen.findByRole('heading', { name: 'Doğrudan gönderilecek taslak' })).toBeInTheDocument()
    expect(screen.getByText('Bşk. Yrd. İncelemesinde')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Yeni Kayıt' }))
    expect(screen.getByLabelText(/Başlık/)).toHaveValue('')
    expect(screen.getByLabelText(/Kategori/)).toHaveValue('')
    expect(screen.getByLabelText(/Açıklama/)).toHaveValue('')
  })

  it('kaydedilmemiş yeni kayıt formu kapatılırken onay ister ve odağı dialog içinde tutar', async () => {
    const user = userEvent.setup()
    await renderEmployeeApp('/dashboard')

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
    await renderEmployeeApp('/kayitlar/rec-002/duzenle')

    const fileInput = await screen.findByLabelText('Dosya ekle')
    const file = new File(['teklif'], 'teklif.pdf', { type: 'application/pdf', lastModified: 10 })
    await user.upload(fileInput, file)
    await user.upload(fileInput, file)

    expect(screen.getByRole('alert')).toHaveTextContent('Aynı dosya birden fazla kez eklenemez')
    expect(screen.getAllByText('teklif.pdf')).toHaveLength(1)
  })
})
