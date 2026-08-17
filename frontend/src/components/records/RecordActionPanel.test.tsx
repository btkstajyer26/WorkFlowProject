import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { http, HttpResponse } from 'msw'
import { MemoryRouter } from 'react-router'
import { describe, expect, it } from 'vitest'
import { api, setApiAccessToken } from '../../api/client'
import { apiBaseUrl } from '../../api/config'
import type { WorkflowActionRequest } from '../../api/generated/data-contracts'
import { ToastProvider } from '../../context/ToastContext'
import { WorkflowProvider } from '../../context/WorkflowContext'
import { useWorkflow } from '../../context/workflowState'
import { getDemoUserByRole } from '../../mocks/users'
import { RecordActionPanel } from './RecordActionPanel'
import type { WorkflowRecord } from '../../types/record'
import { apiMockServer } from '../../mocks/api/server'

function ActionPanelHarness({ recordId = 'rec-003' }: { recordId?: string }) {
  const { user, visibleRecords } = useWorkflow()
  const record = visibleRecords.find((item) => item.id === recordId)
  if (!record) return null
  return (
    <>
      <RecordActionPanel record={record} role={user.role} />
      <span data-testid="latest-history-note">{record.history.at(-1)?.note ?? ''}</span>
    </>
  )
}

function renderActionPanel(role: 'CALISAN' | 'BASKAN', recordId?: string) {
  const user = getDemoUserByRole(role)
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  return render(
    <MemoryRouter>
      <QueryClientProvider client={queryClient}>
        <ToastProvider>
          <WorkflowProvider user={user}>
            <ActionPanelHarness recordId={recordId} />
          </WorkflowProvider>
        </ToastProvider>
      </QueryClientProvider>
    </MemoryRouter>,
  )
}

async function renderBackendChairPanel() {
  const tokens = await api.auth.login({ email: 'mehmet.demir@kurum.gov.tr', password: 'demo123' })
  setApiAccessToken(tokens.accessToken!)
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  const record: WorkflowRecord = {
    id: 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa3',
    recordNumber: '',
    title: 'Bakım sözleşmesi yenileme',
    description: 'Yıllık bakım sözleşmesinin yenilenmesi talebidir.',
    categoryId: 1,
    category: 'İdari',
    status: 'BASKAN_INCELEMESINDE',
    createdBy: '',
    assignedTo: null,
    lastAction: '',
    createdAt: '2026-08-03T11:30:00Z',
    updatedAt: '2026-08-05T09:45:00Z',
    attachments: [],
    history: [],
  }

  render(
    <MemoryRouter initialEntries={[`/kayitlar/${record.id}`]}>
      <QueryClientProvider client={queryClient}>
        <ToastProvider>
          <WorkflowProvider user={getDemoUserByRole('BASKAN')}>
            <RecordActionPanel record={record} role="BASKAN" source="backend" />
          </WorkflowProvider>
        </ToastProvider>
      </QueryClientProvider>
    </MemoryRouter>,
  )
}

describe('RecordActionPanel', () => {
  it('ret açıklamasını işlem penceresinde zorunlu gösterir', async () => {
    const user = userEvent.setup()
    renderActionPanel('BASKAN')

    expect(screen.queryByRole('region', { name: 'Çalışma Notu' })).not.toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Reddet' }))

    const explanation = screen.getByRole('textbox', { name: 'Ret açıklaması *' })
    const confirmButton = screen.getAllByRole('button', { name: 'Reddet' }).at(-1)!
    expect(confirmButton).toBeDisabled()

    await user.type(explanation, 'Bütçe kalemi uygun değil.')
    expect(confirmButton).toBeEnabled()
  })

  it('onay açıklamasını doğrudan işlem geçmişine taşır', async () => {
    const user = userEvent.setup()
    renderActionPanel('BASKAN')

    await user.click(screen.getByRole('button', { name: 'Onayla' }))
    const explanation = screen.getByRole('textbox', { name: 'Onay açıklaması (isteğe bağlı)' })
    expect(explanation).toHaveValue('')
    await user.type(explanation, 'Nihai onay açıklaması.')
    await user.click(screen.getAllByRole('button', { name: 'Onayla' }).at(-1)!)

    await waitFor(() => expect(screen.getByTestId('latest-history-note')).toHaveTextContent('Nihai onay açıklaması.'))
    expect(screen.getByText('Kayıt onaylandı')).toBeInTheDocument()
  })

  it('Çalışanın incelemeye gönderme penceresinde not alanı göstermez', async () => {
    const user = userEvent.setup()
    renderActionPanel('CALISAN', 'rec-006')

    await user.click(screen.getByRole('button', { name: 'İncelemeye Gönder' }))

    expect(screen.getByRole('dialog', { name: 'Başkan Yardımcısına gönder' })).toBeInTheDocument()
    expect(screen.queryByRole('textbox')).not.toBeInTheDocument()
    expect(screen.queryByText(/Gönderim açıklaması/)).not.toBeInTheDocument()
  })

  it('Başkan kararını gerçek workflow endpointine açıklamasıyla gönderir', async () => {
    const user = userEvent.setup()
    let receivedRequest: WorkflowActionRequest | undefined
    apiMockServer.use(
      http.post(`${apiBaseUrl}/api/records/:recordId/workflow/actions`, async ({ params, request }) => {
        receivedRequest = await request.json() as WorkflowActionRequest
        return HttpResponse.json({
          recordId: params.recordId,
          action: receivedRequest.action,
          previousStatus: 'BASKAN_INCELEMESINDE',
          newStatus: 'ONAYLANDI',
          performedBy: 'user-demo-003',
          performedAt: '2026-08-17T11:15:00Z',
        })
      }),
    )
    await renderBackendChairPanel()

    await user.click(screen.getByRole('button', { name: 'Onayla' }))
    await user.type(screen.getByRole('textbox', { name: 'Onay açıklaması (isteğe bağlı)' }), 'Gerçek API onayı.')
    await user.click(screen.getAllByRole('button', { name: 'Onayla' }).at(-1)!)

    await waitFor(() => expect(screen.getByText('Kayıt onaylandı')).toBeInTheDocument())
    expect(receivedRequest).toEqual({
      action: 'ONAYLA',
      comment: 'Gerçek API onayı.',
    })
  })
})
