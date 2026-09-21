import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { apiBaseUrl } from '../../api/config'
import { ToastProvider } from '../../context/ToastContext'
import { apiMockServer } from '../../mocks/api/server'
import type { Subtask, SubtaskListResponse } from '../../types/subtask'
import { RecordSubtasksPanel } from './RecordSubtasksPanel'

function subtask(overrides: Partial<Subtask>): Subtask {
  return {
    id: 'subtask-1',
    parentRecordId: 'rec-parent',
    title: 'Alt görev',
    description: '',
    assignedTo: 'user-a',
    assignedToName: 'Ayşe Yılmaz',
    status: 'DEGERLENDIRME',
    resolutionComment: null,
    createdAt: '2026-09-21T10:00:00Z',
    completedAt: null,
    ...overrides,
  }
}

function mockSubtasks(recordId: string, response: SubtaskListResponse) {
  apiMockServer.use(
    http.get(`${apiBaseUrl}/api/records/:recordId/subtasks`, ({ params }) => (
      params.recordId === recordId ? HttpResponse.json(response) : HttpResponse.json({ approvalPolicy: null, requiredApprovals: null, subtasks: [] })
    )),
  )
}

function renderPanel(recordId: string, currentUserId: string) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <ToastProvider>
        <RecordSubtasksPanel recordId={recordId} currentUserId={currentUserId} />
      </ToastProvider>
    </QueryClientProvider>,
  )
}

describe('RecordSubtasksPanel', () => {
  it('hiç alt görev yoksa hiçbir şey göstermez', async () => {
    mockSubtasks('rec-empty', { approvalPolicy: null, requiredApprovals: null, subtasks: [] })
    const { container } = renderPanel('rec-empty', 'user-a')
    await waitFor(() => expect(container).toBeEmptyDOMElement())
  })

  it('alt görevlerin durumunu ve politikayı listeler', async () => {
    mockSubtasks('rec-parent', {
      approvalPolicy: 'MAJORITY',
      requiredApprovals: 2,
      subtasks: [
        subtask({ id: 's1', title: 'Birinci inceleme', assignedToName: 'Ayşe Yılmaz', status: 'TAMAMLANDI', completedAt: '2026-09-21T11:00:00Z' }),
        subtask({ id: 's2', title: 'İkinci inceleme', assignedToName: 'Burak Demir', status: 'ISLEM' }),
        subtask({ id: 's3', title: 'Üçüncü inceleme', assignedToName: 'Cem Kaya', status: 'REDDEDILDI', resolutionComment: 'Eksik belge.' }),
      ],
    })
    renderPanel('rec-parent', 'user-a')

    expect(await screen.findByRole('heading', { name: 'Alt Görevler' })).toBeInTheDocument()
    expect(screen.getByText('3 alt görevden 2 tanesi sonuçlandı.', { exact: false })).toBeInTheDocument()
    expect(screen.getByText(/Çoğunluk yeterli/)).toBeInTheDocument()
    expect(screen.getByText('Birinci inceleme')).toBeInTheDocument()
    expect(screen.getByText('Eksik belge.')).toBeInTheDocument()
  })

  it('yalnız kendi alt görevi için işlem düğmesi gösterir ve ilerletir', async () => {
    const user = userEvent.setup()
    mockSubtasks('rec-parent', {
      approvalPolicy: 'UNANIMOUS',
      requiredApprovals: 2,
      subtasks: [
        subtask({ id: 's1', title: 'Bana atanan', assignedTo: 'user-a', status: 'DEGERLENDIRME' }),
        subtask({ id: 's2', title: 'Başkasına atanan', assignedTo: 'user-b', status: 'DEGERLENDIRME' }),
      ],
    })
    let receivedBody: unknown
    apiMockServer.use(
      http.post(`${apiBaseUrl}/api/subtasks/:subtaskId/actions`, async ({ request, params }) => {
        receivedBody = await request.json()
        return HttpResponse.json(subtask({ id: params.subtaskId as string, status: 'ISLEM' }))
      }),
    )
    renderPanel('rec-parent', 'user-a')

    await screen.findByText('Bana atanan')
    // Yalnız kendi alt görevim için buton görünür - "Başkasına atanan" satırında düğme yok.
    const advanceButtons = await screen.findAllByRole('button', { name: 'Değerlendirmeyi Tamamla' })
    expect(advanceButtons).toHaveLength(1)

    await user.click(advanceButtons[0])
    expect(screen.getByRole('dialog', { name: 'Değerlendirmeyi Tamamla' })).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'İşlemi Onayla' }))

    await waitFor(() => expect(receivedBody).toEqual({ action: 'DEGERLENDIRMEYI_TAMAMLA' }))
  })

  it('reddetme açıklaması boşken gönderimi engeller', async () => {
    const user = userEvent.setup()
    mockSubtasks('rec-parent', {
      approvalPolicy: 'UNANIMOUS',
      requiredApprovals: 1,
      subtasks: [subtask({ id: 's1', title: 'Onay bekleyen', assignedTo: 'user-a', status: 'ONAY' })],
    })
    renderPanel('rec-parent', 'user-a')

    await user.click(await screen.findByRole('button', { name: 'Reddet' }))
    const confirmButton = screen.getByRole('button', { name: 'İşlemi Onayla' })
    expect(confirmButton).toBeDisabled()

    await user.type(screen.getByRole('textbox', { name: 'Açıklama *' }), 'Eksik veri var.')
    expect(confirmButton).toBeEnabled()
  })
})
