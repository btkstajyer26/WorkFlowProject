import { useCallback, useEffect, useRef, useState } from 'react'

export function useSingleFlight(minimumLockMs = 350) {
  const lockedRef = useRef(false)
  const mountedRef = useRef(true)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    mountedRef.current = true
    return () => {
      mountedRef.current = false
    }
  }, [])

  const run = useCallback(async (action: () => unknown | Promise<unknown>) => {
    if (lockedRef.current) return false
    lockedRef.current = true
    setBusy(true)
    const startedAt = Date.now()

    try {
      await action()
      return true
    } finally {
      const remainingDelay = Math.max(0, minimumLockMs - (Date.now() - startedAt))
      window.setTimeout(() => {
        lockedRef.current = false
        if (mountedRef.current) setBusy(false)
      }, remainingDelay)
    }
  }, [minimumLockMs])

  return { busy, run }
}
