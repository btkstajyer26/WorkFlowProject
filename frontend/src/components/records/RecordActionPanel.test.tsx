import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { WorkflowProvider } from '../../context/WorkflowContext'
import { useWorkflow } from '../../context/workflowState'
import { getDemoUserByRole } from '../../mocks/users'
import { RecordActionPanel } from './RecordActionPanel'

function ActionPanelHarness() {
  const { user, visibleRecords } = useWorkflow()
  const record = visibleRecords.find((item) => item.id === 'rec-003')
  if (!record) return null
  return <RecordActionPanel record={record} role={user.role} />
}

describe('RecordActionPanel', () => {
  it('ret açıklamasını bağımsız not alanından ayrı ve zorunlu gösterir', async () => {
    const user = userEvent.setup()
    const chair = getDemoUserByRole('BASKAN')
    render(
      <WorkflowProvider user={chair}>
        <ActionPanelHarness />
      </WorkflowProvider>,
    )

    expect(screen.queryByText('İnceleme notu')).not.toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Reddet' }))

    const explanation = screen.getByRole('textbox', { name: 'Ret açıklaması *' })
    const confirmButton = screen.getAllByRole('button', { name: 'Reddet' }).at(-1)!
    expect(confirmButton).toBeDisabled()

    await user.type(explanation, 'Bütçe kalemi uygun değil.')
    expect(confirmButton).toBeEnabled()
  })
})
