import { apiBaseUrl, isApiMockEnabled } from '../../api/config'

export async function enableApiMocking() {
  if (!isApiMockEnabled) return

  const { apiMockWorker } = await import('./browser')
  const apiOrigin = new URL(apiBaseUrl).origin
  await apiMockWorker.start({
    onUnhandledRequest(request, print) {
      if (new URL(request.url).origin === apiOrigin) print.warning()
    },
  })
}
