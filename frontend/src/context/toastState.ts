import { createContext, useContext } from 'react'

export type ToastTone = 'success' | 'error' | 'warning' | 'info'

export type ToastInput = {
  title: string
  description?: string
  tone?: ToastTone
  durationMs?: number
}

export type ToastItem = ToastInput & {
  id: string
  tone: ToastTone
}

export type ToastContextValue = {
  showToast: (toast: ToastInput) => string
  dismissToast: (toastId: string) => void
}

export const ToastContext = createContext<ToastContextValue | null>(null)

export function useToast() {
  const context = useContext(ToastContext)
  if (!context) throw new Error('useToast must be used inside ToastProvider.')
  return context
}
