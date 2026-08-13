import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { describe, expect, it } from 'vitest'
import { ToastProvider } from '../../context/ToastContext'
import { WorkflowProvider } from '../../context/WorkflowContext'
import { useWorkflow } from '../../context/workflowState'
import { getDemoUserByRole } from '../../mocks/users'
import { RecordActionPanel } from './RecordActionPanel'

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
  return render(
    <MemoryRouter>
      <ToastProvider>
        <WorkflowProvider user={user}>
          <ActionPanelHarness recordId={recordId} />
        </WorkflowProvider>
      </ToastProvider>
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
})
