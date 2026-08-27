import { apiHttpClient } from './client'

/**
 * E-posta bildirimindeki "Hızlı İşlem" bağlantısının arkasındaki iki uç.
 *
 * İkisi de oturum gerektirmez (`secure` verilmez): kimlik, istekte taşınan tek
 * kullanımlık anahtardan gelir. Anahtar gövdede gider, adres çubuğunda değil —
 * proje tek kullanımlık anahtarları URL'ye yazmama kararını şifre sıfırlama
 * akışında zaten vermişti.
 */

export type MailActionPreview = {
  recordId: string
  recordTitle: string | null
  recordStatus: string | null
  action: string
  recipientName: string | null
  expiresAt: string
}

export type MailActionResult = {
  recordId: string
}

/** Bağlantıyı doğrular ve onay ekranı bilgisini getirir. Durum değiştirmez. */
export function previewMailAction(token: string) {
  return apiHttpClient.request<MailActionPreview, unknown>({
    path: '/api/public/mail-actions/preview',
    method: 'POST',
    body: { token },
    type: 'application/json',
    format: 'json',
  })
}

/** Bağlantıyı tüketir ve workflow aksiyonunu yürütür. */
export function consumeMailAction(token: string) {
  return apiHttpClient.request<MailActionResult, unknown>({
    path: '/api/public/mail-actions/consume',
    method: 'POST',
    body: { token },
    type: 'application/json',
    format: 'json',
  })
}
