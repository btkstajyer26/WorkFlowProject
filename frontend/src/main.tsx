import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router'
import './index.css'
import App from './App.tsx'

async function bootstrap() {
  if (import.meta.env.VITE_USE_MOCKS === 'true') {
    const { browserMockWorker } = await import('./mocks/api/browser')
    await browserMockWorker.start({ onUnhandledRequest: 'bypass' })
  }

  createRoot(document.getElementById('root')!).render(
    <StrictMode>
      <BrowserRouter>
        <App />
      </BrowserRouter>
    </StrictMode>,
  )
}

void bootstrap()
