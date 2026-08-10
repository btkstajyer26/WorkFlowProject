import { Moon, Sun } from 'lucide-react'
import { useTheme } from '../../context/themeState'

export function ThemeToggle() {
  const { theme, toggleTheme } = useTheme()
  const dark = theme === 'dark'

  return (
    <button
      type="button"
      role="switch"
      aria-checked={dark}
      onClick={toggleTheme}
      className="flex min-h-11 w-full items-center gap-3 rounded-xl px-3 text-left text-sm font-semibold text-app-text-muted transition-colors hover:bg-app-surface-strong hover:text-app-text-strong focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
    >
      <span className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-app-surface text-brand-700 dark:text-brand-300 shadow-sm ring-1 ring-app-border dark:text-brand-300">
        {dark
          ? <Moon className="size-4" aria-hidden="true" />
          : <Sun className="size-4" aria-hidden="true" />}
      </span>
      <span className="min-w-0 flex-1">Koyu tema</span>
      <span
        aria-hidden="true"
        className={`relative h-6 w-11 shrink-0 rounded-full transition-colors ${dark ? 'bg-brand-600' : 'bg-app-text-disabled'}`}
      >
        <span
          className={`absolute top-1 size-4 rounded-full bg-app-surface shadow-sm transition-transform motion-reduce:transition-none ${dark ? 'translate-x-6' : 'translate-x-1'}`}
        />
      </span>
    </button>
  )
}
