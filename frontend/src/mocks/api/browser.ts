import { setupWorker } from 'msw/browser'
import { apiHandlers } from './handlers'
import { restoreMockApiDb, snapshotMockApiDb } from './db'

/**
 * Tarayıcıda (dev sunucusunda) mock backend - Vitest'in `msw/node` sunucusuyla
 * (server.ts) aynı `apiHandlers`'ı paylaşır. Yalnız `VITE_USE_MOCKS=true`
 * iken main.tsx tarafından başlatılır; gerçek backend'e hiç bağlanmadan
 * subtask gibi henüz backend'i olmayan özellikleri denemek için kullanılır.
 *
 * Mock veritabanı normalde yalnız sayfanın kendi JS belleğinde durur - çıkış/
 * giriş gibi tam sayfa yenilemesi yapan akışlarda sıfırlanırdı. Burada
 * localStorage'a yansıtılıp geri yükleniyor ki hesap değiştirirken (ör. bir
 * alt görevi Başkan Yardımcısı olarak oluşturup sonra o kişinin hesabına
 * geçince) veri kaybolmasın.
 */
const STORAGE_KEY = 'ebys-mock-db-v1'

function hydrateFromStorage() {
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY)
    if (!raw) return
    restoreMockApiDb(JSON.parse(raw))
  } catch {
    // Bozuk veya eski şemalı bir kayıt varsa sessizce yok say, seed veriyle devam et.
  }
}

function persistToStorage() {
  try {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(snapshotMockApiDb()))
  } catch {
    // localStorage kullanılamıyorsa (gizli sekme, kota vb.) sessizce yut.
  }
}

hydrateFromStorage()

export const browserMockWorker = setupWorker(...apiHandlers)
browserMockWorker.events.on('response:mocked', persistToStorage)

// Konsoldan "mock verilerini sıfırla" için: window.__resetMockDb()
declare global {
  interface Window {
    __resetMockDb?: () => void
  }
}
window.__resetMockDb = () => {
  window.localStorage.removeItem(STORAGE_KEY)
  window.location.reload()
}
