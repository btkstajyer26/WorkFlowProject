import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
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
    await user.selectOptions(screen.getByLabelText(/Kategori/), '4')
    await user.type(screen.getByLabelText(/Açıklama/), 'Yeni kayıt açıklaması')
    await user.click(screen.getByRole('button', { name: 'Taslak Kaydet' }))
    await user.click(await screen.findByRole('button', { name: 'Yeni Kayıt Oluştur' }))

    expect(screen.getByLabelText(/Başlık/)).toHaveValue('')
    expect(screen.getByLabelText(/Kategori/)).toHaveValue('0')
    expect(screen.getByLabelText(/Açıklama/)).toHaveValue('')
    expect(screen.getByRole('button', { name: 'Taslak Kaydet' })).toBeInTheDocument()
  })

  it('kaydedilen taslağı doğrudan incelemeye gönderir', async () => {
    const user = userEvent.setup()
    await renderEmployeeApp('/dashboard')

    await user.click(await screen.findByRole('button', { name: 'Yeni Kayıt' }))
    await user.type(screen.getByLabelText(/Başlık/), 'Doğrudan gönderilecek taslak')
    await user.selectOptions(screen.getByLabelText(/Kategori/), '4')
    await user.type(screen.getByLabelText(/Açıklama/), 'Taslak kaydedildikten sonra incelemeye gönderilecek.')
    await user.click(screen.getByRole('button', { name: 'Taslak Kaydet' }))
    const submitButton = await screen.findByRole('button', { name: 'İncelemeye Gönder' })
    await waitFor(() => expect(submitButton).toBeEnabled())
    await user.click(submitButton)

    expect(await screen.findByRole('heading', { name: 'Doğrudan gönderilecek taslak' })).toBeInTheDocument()
    expect(screen.getByText('Bşk. Yrd. İncelemesinde')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Yeni Kayıt' }))
    expect(screen.getByLabelText(/Başlık/)).toHaveValue('')
    expect(screen.getByLabelText(/Kategori/)).toHaveValue('0')
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

  it('yeni kayıt alanına desteklenen dosyaları sürükleyip bırakır', async () => {
    const user = userEvent.setup()
    await renderEmployeeApp('/dashboard')

    await user.click(await screen.findByRole('button', { name: 'Yeni Kayıt' }))
    const dropZone = screen.getByRole('group', { name: 'Ek dosya yükleme alanı' })
    const files = [
      new File(['pdf'], 'belge.pdf', { type: 'application/pdf' }),
      new File(['docx'], 'rapor.docx', { type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' }),
      new File(['doc'], 'eski-rapor.doc', { type: 'application/msword' }),
      new File(['xlsx'], 'tablo.xlsx', { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' }),
      new File(['xls'], 'eski-tablo.xls', { type: 'application/vnd.ms-excel' }),
      new File(['png'], 'gorsel.png', { type: 'image/png' }),
      new File(['jpeg'], 'fotograf.jpeg', { type: 'image/jpeg' }),
      new File(['jpg'], 'tarama.jpg', { type: 'image/jpeg' }),
    ]
    const dataTransfer = { files, types: ['Files'], dropEffect: 'none' }

    fireEvent.dragEnter(dropZone, { dataTransfer })
    expect(screen.getByText('Dosyaları bırakın')).toBeInTheDocument()
    fireEvent.drop(dropZone, { dataTransfer })

    const attachmentList = screen.getByRole('list', { name: 'Eklenen dosyalar' })
    files.forEach((file) => expect(within(attachmentList).getByText(file.name)).toBeInTheDocument())
    expect(screen.queryByText('Dosyaları bırakın')).not.toBeInTheDocument()
  })

  it('sürüklenen desteklenmeyen dosyayı reddeder ve tekrar dosya eklemez', async () => {
    const user = userEvent.setup()
    await renderEmployeeApp('/dashboard')

    await user.click(await screen.findByRole('button', { name: 'Yeni Kayıt' }))
    const dropZone = screen.getByRole('group', { name: 'Ek dosya yükleme alanı' })
    const pdf = new File(['pdf'], 'teklif.pdf', { type: 'application/pdf', lastModified: 10 })

    fireEvent.drop(dropZone, { dataTransfer: { files: [pdf], types: ['Files'] } })
    fireEvent.drop(dropZone, { dataTransfer: { files: [pdf], types: ['Files'] } })
    expect(screen.getByRole('alert')).toHaveTextContent('Aynı dosya birden fazla kez eklenemez')
    expect(screen.getAllByText('teklif.pdf')).toHaveLength(1)

    const unsupportedFile = new File(['text'], 'notlar.txt', { type: 'text/plain' })
    fireEvent.drop(dropZone, { dataTransfer: { files: [unsupportedFile], types: ['Files'] } })
    expect(screen.getByRole('alert')).toHaveTextContent('“notlar.txt” desteklenmiyor')
    expect(screen.queryByText('notlar.txt')).not.toBeInTheDocument()
  })

  it('düzeltme bekleyen kaydın düzenleme ekranında düzeltme talebini gösterir ve yeniden gönderir', async () => {
    const user = userEvent.setup()
    await renderEmployeeApp('/kayitlar/rec-002/duzenle')

    expect(await screen.findByRole('heading', { name: 'Düzeltmeleri Tamamla' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Düzeltme Talebi' })).toBeInTheDocument()
    expect(screen.getByText('Lisans adedi ve kullanım süresi bilgisi eklenmelidir.')).toBeInTheDocument()

    const submitButton = screen.getByRole('button', { name: 'Yeniden Gönder' })
    expect(submitButton).toBeInTheDocument()

    await user.clear(screen.getByLabelText(/Kayıt açıklaması/))
    await user.type(screen.getByLabelText(/Kayıt açıklaması/), 'Tasarım lisansı için 5 adet 1 yıllık lisans talep edilmektedir.')

    await user.click(submitButton)

    const dialog = screen.getByRole('dialog')
    expect(dialog).toBeInTheDocument()
    const confirmBtn = within(dialog).getByRole('button', { name: 'Yeniden Gönder' })
    expect(confirmBtn).toBeInTheDocument()

    await user.click(confirmBtn)

    await waitFor(() => {
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    })

    expect(await screen.findByRole('heading', { name: 'Yazılım Lisansı Talebi' })).toBeInTheDocument()
    expect(screen.getByText('Bşk. Yrd. İncelemesinde')).toBeInTheDocument()
  })
})


