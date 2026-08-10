import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { ToastProvider } from '../../context/ToastContext'
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

function renderNotesHarness(role: 'CALISAN' | 'BASKAN') {
  const user = getDemoUserByRole(role)
  return render(
    <ToastProvider>
      <WorkflowProvider user={user}>
        <NotesHarness />
      </WorkflowProvider>
    </ToastProvider>,
  )
}

describe('RecordNotesPanel', () => {
  it('atanmış incelemecinin özel çalışma notunu ekleyip düzenlemesini sağlar', async () => {
    const user = userEvent.setup()
    renderNotesHarness('BASKAN')

    expect(screen.getByRole('region', { name: 'Çalışma Notu' })).toBeInTheDocument()
    expect(screen.getByText('Henüz bir çalışma notunuz bulunmuyor.')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Not Ekle' }))
    await user.type(screen.getByRole('textbox', { name: 'İnceleme notunuzu yazın' }), 'Başkan değerlendirmesi')
    await user.click(screen.getByRole('button', { name: 'Notu Kaydet' }))

    expect(await screen.findByText('Başkan değerlendirmesi')).toBeInTheDocument()
    expect(screen.getByRole('status')).toHaveTextContent('Çalışma notunuz kaydedildi')
    expect(screen.getByText('Özel taslağınız')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Notumu Düzenle' }))
    const textbox = screen.getByRole('textbox', { name: 'Notunuzu düzenleyin' })
    await user.clear(textbox)
    await user.type(textbox, 'Düzeltilmiş Başkan değerlendirmesi')
    const saveButton = screen.getByRole('button', { name: 'Değişiklikleri Kaydet' })
    await waitFor(() => expect(saveButton).toBeEnabled())
    await user.click(saveButton)

    expect(await screen.findByText('Düzeltilmiş Başkan değerlendirmesi')).toBeInTheDocument()
    expect(screen.queryByText('Başkan değerlendirmesi')).not.toBeInTheDocument()
    expect(screen.getAllByText('Düzeltilmiş Başkan değerlendirmesi')).toHaveLength(1)
  })

  it('çalışana başka kullanıcıların çalışma notu panelini göstermez', () => {
    const { container } = renderNotesHarness('CALISAN')

    expect(container.querySelector('[aria-labelledby="record-notes-title"]')).not.toBeInTheDocument()
  })
})
