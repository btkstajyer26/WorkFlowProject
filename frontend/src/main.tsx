import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router'
import './index.css'
import App from './App.tsx'

async function enableDevelopmentApiMocking() {
  if (!import.meta.env.DEV) return
  const { enableApiMocking } = await import('./mocks/api/enable')
  await enableApiMocking()
}

async function bootstrap() {
  await enableDevelopmentApiMocking()

  createRoot(document.getElementById('root')!).render(
    <StrictMode>
      <BrowserRouter>
        <App />
      </BrowserRouter>
    </StrictMode>,
  )
}

void bootstrap()
