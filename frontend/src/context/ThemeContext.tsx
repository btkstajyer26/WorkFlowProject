import { useEffect, useState, type ReactNode } from 'react'
import { ThemeContext, type Theme } from './themeState'

export const themeStorageKey = 'ebys:theme:v1'

function getInitialTheme(): Theme {
  try {
    const storedTheme = window.localStorage.getItem(themeStorageKey)
    if (storedTheme === 'light' || storedTheme === 'dark') return storedTheme
  } catch {
    // Depolama kapalıysa başlangıç class'ı veya sistem tercihi kullanılır.
  }

  if (document.documentElement.classList.contains('dark')) return 'dark'
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

function applyTheme(theme: Theme) {
  const root = document.documentElement
  root.classList.toggle('dark', theme === 'dark')
  root.style.colorScheme = theme
}

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [theme, setTheme] = useState<Theme>(getInitialTheme)

  useEffect(() => {
    applyTheme(theme)
  }, [theme])

  const toggleTheme = () => {
    const nextTheme = theme === 'dark' ? 'light' : 'dark'
    applyTheme(nextTheme)
    setTheme(nextTheme)
    try {
      window.localStorage.setItem(themeStorageKey, nextTheme)
    } catch {
      // Tema depolama kullanılamasa da mevcut oturumda tema değişmeye devam eder.
    }
  }

  return (
    <ThemeContext.Provider value={{ theme, toggleTheme }}>
      {children}
    </ThemeContext.Provider>
  )
}
