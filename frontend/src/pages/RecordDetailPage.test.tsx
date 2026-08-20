import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { describe, expect, it } from 'vitest'
import App from '../App'
import { seedAuthenticatedUser } from '../test/auth'
import { mockApiDb } from '../mocks/api/db'

async function renderRecordDetail(role: 'CALISAN' | 'BASKAN_YARDIMCISI' | 'BASKAN', recordId: string) {
  await seedAuthenticatedUser(role)
  return render(
    <MemoryRouter initialEntries={[`/kayitlar/${recordId}`]}>
      <App />
    </MemoryRouter>,
  )
}

describe('RecordDetailPage', () => {
  it('durum rozetini rol değişse de kayıt başlığının bulunduğu header içinde gösterir', async () => {
    await renderRecordDetail('BASKAN', 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa3')

    const title = await screen.findByRole('heading', { name: 'Bakım sözleşmesi yenileme' })
    const header = title.closest('header')

    expect(header).not.toBeNull()
    expect(within(header!).getByText('Başkan İncelemesinde')).toBeInTheDocument()
  })

  it('Başkana iletilen son işlem notunu koşullu panelde gösterir', async () => {
    const user = userEvent.setup()
    mockApiDb.auditLogs = [{
      id: 'audit-chair-note',
      recordId: 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa3',
      action: 'BASKANA_ILET',
      userId: 'user-demo-002',
      userFullName: 'Ayşe Kaya',
      roleName: 'BASKAN_YARDIMCISI',
      comment: 'Teknik plan ve bütçe kalemleri kontrol edildi.',
      createdAt: '2026-08-05T09:45:00Z',
    }]
    await renderRecordDetail('BASKAN', 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa3')

    const noteTitle = await screen.findByRole('heading', { name: 'Son İşlem Notu' })
    const noteDetails = noteTitle.closest('details')
    expect(noteDetails).not.toHaveAttribute('open')

    await user.click(noteTitle.closest('summary')!)
    expect(noteDetails).toHaveAttribute('open')
    expect(within(noteDetails!).getByText(/Teknik plan ve bütçe kalemleri kontrol edildi/)).toBeVisible()
  })

  it('Çalışandan not gelmediği için Başkan Yardımcısı görünümünde not paneli oluşturmaz', async () => {
    await renderRecordDetail('BASKAN_YARDIMCISI', 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa2')

    expect(await screen.findByRole('heading', { name: 'Birim içi eğitim planı' })).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Son İşlem Notu' })).not.toBeInTheDocument()
  })

  it('işlem geçmişini başlangıçta kapalı tutar ve istek üzerine açar', async () => {
    const user = userEvent.setup()
    mockApiDb.auditLogs = [
      {
        id: 'audit-created',
        recordId: 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa1',
        action: 'RECORD_CREATED',
        userId: 'user-demo-001',
        userFullName: 'John Doe',
        roleName: 'CALISAN',
        createdAt: '2026-08-01T09:15:00Z',
      },
      {
        id: 'audit-submitted',
        recordId: 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa1',
        action: 'GONDER',
        userId: 'user-demo-001',
        userFullName: 'John Doe',
        roleName: 'CALISAN',
        createdAt: '2026-08-01T10:15:00Z',
      },
    ]
    await renderRecordDetail('CALISAN', 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa1')

    const historyTitle = await screen.findByRole('heading', { name: 'İşlem Geçmişi' })
    const historyDetails = historyTitle.closest('details')
    expect(historyDetails).not.toHaveAttribute('open')
    expect(within(historyDetails!).getByText('2 hareket')).toBeInTheDocument()

    await user.click(historyTitle.closest('summary')!)
    expect(historyDetails).toHaveAttribute('open')

    const actions = within(historyDetails!).getAllByText(/gönderildi|oluşturuldu/i)
    expect(actions[0]).toHaveTextContent('Başkan Yardımcısına gönderildi')
    expect(actions[1]).toHaveTextContent('Kayıt oluşturuldu')
  })

  it('Başkan ve Başkan Yardımcısı için yalnızca durumlarına uygun kararları gösterir', async () => {
    const deputyView = await renderRecordDetail('BASKAN_YARDIMCISI', 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa2')
    const deputyActions = await screen.findByRole('region', { name: 'Karar' })
    expect(within(deputyActions).getByRole('button', { name: 'Geri Gönder' })).toBeInTheDocument()
    expect(within(deputyActions).getByRole('button', { name: 'Başkana İlet' })).toBeInTheDocument()
    expect(within(deputyActions).queryByRole('button', { name: 'Onayla' })).not.toBeInTheDocument()
    deputyView.unmount()

    await renderRecordDetail('BASKAN', 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa3')
    const chairActions = await screen.findByRole('region', { name: 'Karar' })
    expect(within(chairActions).getByRole('button', { name: 'Geri Gönder' })).toBeInTheDocument()
    expect(within(chairActions).getByRole('button', { name: 'Reddet' })).toBeInTheDocument()
    expect(within(chairActions).getByRole('button', { name: 'Onayla' })).toBeInTheDocument()
    expect(within(chairActions).queryByRole('button', { name: 'Başkana İlet' })).not.toBeInTheDocument()
  })
})
