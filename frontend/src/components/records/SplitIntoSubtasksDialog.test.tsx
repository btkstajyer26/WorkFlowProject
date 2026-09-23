import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { apiBaseUrl } from '../../api/config'
import { ToastProvider } from '../../context/ToastContext'
import { apiMockServer } from '../../mocks/api/server'
import type { SplitIntoSubtasksRequest } from '../../types/subtask'
import { SplitIntoSubtasksDialog } from './SplitIntoSubtasksDialog'

function mockAssignableUsers(recordId: string) {
  apiMockServer.use(
    http.get(`${apiBaseUrl}/api/records/:recordId/subtasks/assignable-users`, ({ params }) => (
      params.recordId === recordId
        ? HttpResponse.json({ users: [
            { id: 'user-a', fullName: 'Ayşe Yılmaz' },
            { id: 'user-b', fullName: 'Burak Demir' },
            { id: 'user-c', fullName: 'Cem Kaya' },
          ] })
        : HttpResponse.json({ users: [] })
    )),
  )
}

function renderDialog(recordId = 'rec-parent') {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  const onClose = () => {}
  return render(
    <QueryClientProvider client={queryClient}>
      <ToastProvider>
        <SplitIntoSubtasksDialog recordId={recordId} open onClose={onClose} />
      </ToastProvider>
    </QueryClientProvider>,
  )
}

describe('SplitIntoSubtasksDialog', () => {
  it('başlangıçta iki satır ve "Tümü onaylanmalı" politikasıyla açılır', async () => {
    mockAssignableUsers('rec-parent')
    renderDialog()

    expect(await screen.findByText('Alt Görev 1')).toBeInTheDocument()
    expect(screen.getByText('Alt Görev 2')).toBeInTheDocument()
    expect(screen.queryByText('Alt Görev 3')).not.toBeInTheDocument()
    expect(screen.getByRole('radio', { name: /Tümü onaylanmalı/ })).toBeChecked()
  })

  it('kişi seçilmeden gönderim reddedilir', async () => {
    const user = userEvent.setup()
    mockAssignableUsers('rec-parent')
    renderDialog()

    await screen.findByText('Alt Görev 1')
    for (const input of screen.getAllByPlaceholderText('Başlık')) {
      await user.type(input, 'Bir başlık')
    }
    await user.click(screen.getByRole('button', { name: 'Alt Görevlere Ayır' }))

    expect(await screen.findByText('Her alt görev için bir kişi seçin')).toBeInTheDocument()
  })

  it('aynı kişi iki alt göreve atanamaz', async () => {
    const user = userEvent.setup()
    mockAssignableUsers('rec-parent')
    renderDialog()

    await screen.findByText('Alt Görev 1')
    const titles = screen.getAllByPlaceholderText('Başlık')
    const selects = screen.getAllByRole('combobox')
    for (const input of titles) await user.type(input, 'Bir başlık')
    for (const select of selects) await user.selectOptions(select, 'user-a')

    await user.click(screen.getByRole('button', { name: 'Alt Görevlere Ayır' }))

    expect(await screen.findByText('Aynı kişi birden fazla alt göreve atanamaz')).toBeInTheDocument()
  })

  it('geçerli veriyle bölme isteğini doğru gövdeyle gönderir', async () => {
    const user = userEvent.setup()
    mockAssignableUsers('rec-parent')
    let receivedBody: SplitIntoSubtasksRequest | undefined
    apiMockServer.use(
      http.post(`${apiBaseUrl}/api/records/:recordId/subtasks/split`, async ({ request, params }) => {
        receivedBody = await request.json() as SplitIntoSubtasksRequest
        return HttpResponse.json({
          parentRecordId: params.recordId,
          approvalPolicy: receivedBody.approvalPolicy,
          requiredApprovals: 2,
          subtasks: [],
        }, { status: 201 })
      }),
    )
    renderDialog()

    await screen.findByText('Alt Görev 1')
    const titles = screen.getAllByPlaceholderText('Başlık')
    await user.type(titles[0], 'Birinci inceleme')
    await user.type(titles[1], 'İkinci inceleme')
    const selects = screen.getAllByRole('combobox')
    await user.selectOptions(selects[0], 'user-a')
    await user.selectOptions(selects[1], 'user-b')
    await user.click(screen.getByRole('radio', { name: /Çoğunluk yeterli/ }))

    await user.click(screen.getByRole('button', { name: 'Alt Görevlere Ayır' }))

    await waitFor(() => expect(receivedBody).toEqual({
      approvalPolicy: 'MAJORITY',
      subtasks: [
        { title: 'Birinci inceleme', description: '', assignedTo: 'user-a' },
        { title: 'İkinci inceleme', description: '', assignedTo: 'user-b' },
      ],
    }))
  })

  it('alt görev ekle düğmesi yeni bir satır açar', async () => {
    const user = userEvent.setup()
    mockAssignableUsers('rec-parent')
    renderDialog()

    await screen.findByText('Alt Görev 1')
    await user.click(screen.getByRole('button', { name: 'Alt görev ekle' }))

    expect(await screen.findByText('Alt Görev 3')).toBeInTheDocument()
  })
})
