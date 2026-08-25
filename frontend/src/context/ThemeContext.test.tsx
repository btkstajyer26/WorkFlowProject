import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it } from 'vitest'
import { ThemeToggle } from '../components/layout/ThemeToggle'
import { ThemeProvider, themeStorageKey } from './ThemeContext'

afterEach(() => {
  window.localStorage.clear()
  document.documentElement.classList.remove('dark')
  document.documentElement.style.colorScheme = ''
})

describe('ThemeProvider', () => {
  it('kayıtlı koyu temayı uygular ve toggle ile açık temaya geçirir', async () => {
    window.localStorage.setItem(themeStorageKey, 'dark')
    const user = userEvent.setup()

    render(
      <ThemeProvider>
        <ThemeToggle />
      </ThemeProvider>,
    )

    const toggle = screen.getByRole('switch', { name: 'Koyu tema' })
    expect(toggle).toHaveAttribute('aria-checked', 'true')
    await waitFor(() => expect(document.documentElement).toHaveClass('dark'))

    await user.click(toggle)

    expect(toggle).toHaveAttribute('aria-checked', 'false')
    expect(document.documentElement).not.toHaveClass('dark')
    expect(document.documentElement.style.colorScheme).toBe('light')
    expect(window.localStorage.getItem(themeStorageKey)).toBe('light')
  })
})
