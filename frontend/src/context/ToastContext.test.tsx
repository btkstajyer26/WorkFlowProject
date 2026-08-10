import { act, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { ToastProvider } from './ToastContext'
import { useToast } from './toastState'

function ToastHarness() {
  const { showToast } = useToast()
  return (
    <div>
      <button type="button" onClick={() => showToast({ title: 'Kaydedildi', tone: 'success' })}>
        Başarı göster
      </button>
      <button type="button" onClick={() => showToast({ title: 'İşlem başarısız', tone: 'error' })}>
        Hata göster
      </button>
    </div>
  )
}

afterEach(() => {
  vi.useRealTimers()
})

describe('ToastProvider', () => {
  it('başarı bildirimini gösterip otomatik kapatır', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true })
    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
    render(<ToastProvider><ToastHarness /></ToastProvider>)

    await user.click(screen.getByRole('button', { name: 'Başarı göster' }))
    expect(screen.getByRole('status')).toHaveTextContent('Kaydedildi')

    act(() => vi.advanceTimersByTime(4000))
    expect(screen.queryByRole('status')).not.toBeInTheDocument()
  })

  it('hata bildirimini kullanıcı kapatana kadar tutar', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true })
    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
    render(<ToastProvider><ToastHarness /></ToastProvider>)

    await user.click(screen.getByRole('button', { name: 'Hata göster' }))
    act(() => vi.advanceTimersByTime(10000))
    expect(screen.getByRole('alert')).toHaveTextContent('İşlem başarısız')

    await user.click(screen.getByRole('button', { name: 'Bildirimi kapat' }))
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })
})
