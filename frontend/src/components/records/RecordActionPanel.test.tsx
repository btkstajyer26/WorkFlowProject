import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { ToastProvider } from '../../context/ToastContext'
import { WorkflowProvider } from '../../context/WorkflowContext'
import { useWorkflow } from '../../context/workflowState'
import { getDemoUserByRole } from '../../mocks/users'
import { RecordActionPanel } from './RecordActionPanel'
import { RecordNotesPanel } from './RecordNotesPanel'

function ActionPanelHarness() {
  const { user, visibleRecords } = useWorkflow()
  const record = visibleRecords.find((item) => item.id === 'rec-003')
  if (!record) return null
  return (
    <>
      <RecordNotesPanel record={record} />
      <RecordActionPanel record={record} role={user.role} />
      <span data-testid="working-note-count">{record.notes.length}</span>
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
  it('ret açıklamasını bağımsız not alanından ayrı ve zorunlu gösterir', async () => {
    const user = userEvent.setup()
    renderChairHarness()

    expect(screen.getByRole('region', { name: 'Çalışma Notu' })).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Reddet' }))

    const explanation = screen.getByRole('textbox', { name: 'Ret açıklaması *' })
    const confirmButton = screen.getAllByRole('button', { name: 'Reddet' }).at(-1)!
    expect(confirmButton).toBeDisabled()

    await user.type(explanation, 'Bütçe kalemi uygun değil.')
    expect(confirmButton).toBeEnabled()
  })

  it('çalışma notunu işlem açıklamasına taşır ve başarılı işlemden sonra taslağı temizler', async () => {
    const user = userEvent.setup()
    renderChairHarness()

    await user.click(screen.getByRole('button', { name: 'Not Ekle' }))
    await user.type(screen.getByRole('textbox', { name: 'İnceleme notunuzu yazın' }), 'Başkan çalışma notu.')
    await user.click(screen.getByRole('button', { name: 'Notu Kaydet' }))
    expect(screen.getByTestId('working-note-count')).toHaveTextContent('1')

    await user.click(screen.getByRole('button', { name: 'Onayla' }))
    const explanation = screen.getByRole('textbox', { name: 'İşlem açıklaması (isteğe bağlı)' })
    expect(explanation).toHaveValue('Başkan çalışma notu.')
    await user.clear(explanation)
    await user.type(explanation, 'Nihai onay açıklaması.')
    await user.click(screen.getAllByRole('button', { name: 'Onayla' }).at(-1)!)

    await waitFor(() => expect(screen.getByTestId('working-note-count')).toHaveTextContent('0'))
    expect(screen.getByTestId('latest-history-note')).toHaveTextContent('Nihai onay açıklaması.')
    expect(screen.queryByRole('region', { name: 'Çalışma Notu' })).not.toBeInTheDocument()
    expect(screen.getByText('Kayıt onaylandı')).toBeInTheDocument()
  })
})
