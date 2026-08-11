import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { ToastProvider } from '../../context/ToastContext'
import { WorkflowProvider } from '../../context/WorkflowContext'
import { useWorkflow } from '../../context/workflowState'
import { getDemoUserByRole } from '../../mocks/users'
import { RecordActionPanel } from './RecordActionPanel'

function ActionPanelHarness() {
  const { user, visibleRecords } = useWorkflow()
  const record = visibleRecords.find((item) => item.id === 'rec-003')
  if (!record) return null
  return (
    <>
      <RecordActionPanel record={record} role={user.role} />
      <span data-testid="latest-history-note">{record.history.at(-1)?.note ?? ''}</span>
    </>
  )
}

function renderChairHarness() {
  const chair = getDemoUserByRole('BASKAN')
  return render(
    <ToastProvider>
      <WorkflowProvider user={chair}>
        <ActionPanelHarness />
      </WorkflowProvider>
    </ToastProvider>,
  )
}

describe('RecordActionPanel', () => {
  it('ret açıklamasını işlem penceresinde zorunlu gösterir', async () => {
    const user = userEvent.setup()
    renderChairHarness()

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
    renderChairHarness()

    await user.click(screen.getByRole('button', { name: 'Onayla' }))
    const explanation = screen.getByRole('textbox', { name: 'Onay açıklaması (isteğe bağlı)' })
    expect(explanation).toHaveValue('')
    await user.type(explanation, 'Nihai onay açıklaması.')
    await user.click(screen.getAllByRole('button', { name: 'Onayla' }).at(-1)!)

    await waitFor(() => expect(screen.getByTestId('latest-history-note')).toHaveTextContent('Nihai onay açıklaması.'))
    expect(screen.getByText('Kayıt onaylandı')).toBeInTheDocument()
  })
})
