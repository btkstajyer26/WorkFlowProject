import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useState } from 'react'
import { describe, expect, it } from 'vitest'
import { useSingleFlight } from './useSingleFlight'

function SingleFlightHarness() {
  const [count, setCount] = useState(0)
  const { busy, run } = useSingleFlight()
  return (
    <div>
      <output>{count}</output>
      <button
        type="button"
        disabled={busy}
        onClick={() => run(async () => {
          setCount((current) => current + 1)
          await new Promise((resolve) => window.setTimeout(resolve, 50))
        })}
      >
        Kaydet
      </button>
    </div>
  )
}

describe('useSingleFlight', () => {
  it('çift tıklamada mutasyonu yalnız bir kez çalıştırır', async () => {
    const user = userEvent.setup()
    render(<SingleFlightHarness />)
    await user.dblClick(screen.getByRole('button', { name: 'Kaydet' }))
    expect(screen.getByText('1')).toBeInTheDocument()
  })
})
