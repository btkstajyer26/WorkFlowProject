import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { ToastViewport } from '../components/feedback/ToastViewport'
import { ToastContext, type ToastInput, type ToastItem } from './toastState'

const maximumVisibleToasts = 4
const defaultDurationMs = 4000

function createToastId() {
  return typeof crypto.randomUUID === 'function'
    ? crypto.randomUUID()
    : `toast-${Date.now()}-${Math.random().toString(16).slice(2)}`
}

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<ToastItem[]>([])
  const timersRef = useRef(new Map<string, number>())

  const dismissToast = useCallback((toastId: string) => {
    const timer = timersRef.current.get(toastId)
    if (timer !== undefined) window.clearTimeout(timer)
    timersRef.current.delete(toastId)
    setToasts((current) => current.filter((toast) => toast.id !== toastId))
  }, [])

  const showToast = useCallback((input: ToastInput) => {
    const id = createToastId()
    const tone = input.tone ?? 'info'
    const toast: ToastItem = { ...input, id, tone }

    setToasts((current) => [...current, toast].slice(-maximumVisibleToasts))

    const durationMs = input.durationMs ?? (tone === 'error' ? 0 : defaultDurationMs)
    if (durationMs > 0) {
      const timer = window.setTimeout(() => dismissToast(id), durationMs)
      timersRef.current.set(id, timer)
    }

    return id
  }, [dismissToast])

  useEffect(() => () => {
    timersRef.current.forEach((timer) => window.clearTimeout(timer))
    timersRef.current.clear()
  }, [])

  const value = useMemo(() => ({ showToast, dismissToast }), [dismissToast, showToast])

  return (
    <ToastContext.Provider value={value}>
      {children}
      <ToastViewport toasts={toasts} onDismiss={dismissToast} />
    </ToastContext.Provider>
  )
}
