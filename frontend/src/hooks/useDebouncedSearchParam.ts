import { useEffect, useState } from 'react'

type SetSearchParams = (nextParams: URLSearchParams, options?: { replace?: boolean }) => void

export function useDebouncedSearchParam(
  searchParams: URLSearchParams,
  setSearchParams: SetSearchParams,
  delay = 350,
) {
  const externalValue = searchParams.get('q') ?? ''
  const [value, setValue] = useState(externalValue)

  useEffect(() => setValue(externalValue), [externalValue])

  useEffect(() => {
    if (value === externalValue) return
    const timeoutId = window.setTimeout(() => {
      const nextParams = new URLSearchParams(searchParams)
      const normalizedValue = value.trim()
      if (normalizedValue) nextParams.set('q', normalizedValue)
      else nextParams.delete('q')
      nextParams.delete('sayfa')
      setSearchParams(nextParams, { replace: true })
    }, delay)

    return () => window.clearTimeout(timeoutId)
  }, [delay, externalValue, searchParams, setSearchParams, value])

  return [value, setValue] as const
}
