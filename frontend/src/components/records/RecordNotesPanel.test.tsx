import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { WorkflowProvider } from '../../context/WorkflowContext'
import { useWorkflow } from '../../context/workflowState'
import { getDemoUserByRole } from '../../mocks/users'
import { RecordNotesPanel } from './RecordNotesPanel'

function NotesHarness({ recordId = 'rec-003' }: { recordId?: string }) {
  const { visibleRecords } = useWorkflow()
  const record = visibleRecords.find((item) => item.id === recordId)
  if (!record) return null
  return <RecordNotesPanel record={record} />
}

describe('RecordNotesPanel', () => {
  it('aynı kullanıcının notunu tek kartta oluşturup günceller', async () => {
    const user = userEvent.setup()
    const chair = getDemoUserByRole('BASKAN')
    render(
      <WorkflowProvider user={chair}>
        <NotesHarness />
      </WorkflowProvider>,
    )

    expect(screen.getByText('John Doe')).toBeInTheDocument()
    expect(screen.getByText('Ayşe Kaya')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Not Ekle' }))
    await user.type(screen.getByRole('textbox', { name: 'Notunuzu yazın' }), 'Başkan değerlendirmesi')
    await user.click(screen.getByRole('button', { name: 'Notu Kaydet' }))

    expect(await screen.findByText('Başkan değerlendirmesi')).toBeInTheDocument()
    expect(screen.getAllByText('Sizin notunuz')).toHaveLength(1)

    await user.click(screen.getByRole('button', { name: 'Notumu Düzenle' }))
    const editor = screen.getByRole('textbox', { name: 'Notunuzu güncelleyin' })
    await user.clear(editor)
    await user.type(editor, 'Güncellenmiş Başkan değerlendirmesi')
    const saveButton = screen.getByRole('button', { name: 'Değişiklikleri Kaydet' })
    await waitFor(() => expect(saveButton).toBeEnabled())
    await user.click(saveButton)

    const updatedNote = await screen.findByText('Güncellenmiş Başkan değerlendirmesi')
    expect(updatedNote).toBeInTheDocument()
    expect(screen.queryByText('Başkan değerlendirmesi')).not.toBeInTheDocument()
    expect(screen.getAllByText('Sizin notunuz')).toHaveLength(1)
    expect(within(updatedNote.closest('li')!).getByText('Düzenlendi')).toBeInTheDocument()
  })

  it('sonuçlanmış kayıtlardaki not alanını salt okunur gösterir', () => {
    const chair = getDemoUserByRole('BASKAN')
    render(
      <WorkflowProvider user={chair}>
        <NotesHarness recordId="rec-004" />
      </WorkflowProvider>,
    )

    expect(screen.getByText('Sonuçlanan kayıtlarda notlar görüntülenebilir ancak değiştirilemez.')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Not Ekle' })).not.toBeInTheDocument()
  })
})
