import { AlertTriangle, ArrowRight, CheckCircle2, Loader2 } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router'
import { consumeMailAction, previewMailAction, type MailActionPreview } from '../api/mailActions'
import { ApiClientError } from '../api/errors'
import { Brand } from '../components/layout/Brand'

/**
 * E-posta bildirimindeki "Hızlı İşlem" bağlantısının açtığı onay ekranı.
 *
 * Sayfa oturum gerektirmez; kullanıcı doğrudan postadan gelir.
 *
 * İki tasarım kararı bilinçli:
 *
 * 1. Anahtar adres **parçasında** (`#token=`) taşınır, sorgu dizisinde değil.
 *    Parça sunucuya hiç gönderilmez: ne erişim log'una ne `Referer` başlığına
 *    düşer. Sayfa değeri okur okumaz adresten siler, böylece tarayıcı
 *    geçmişinde ve paylaşılan ekran görüntüsünde de kalmaz.
 *
 * 2. Açılış işlemi **yapmaz**, yalnız onaya sunar. Posta ağ geçitleri ve
 *    bağlantı tarayıcıları linkleri kendiliğinden getirir; işlem açılışta
 *    yürütülseydi kullanıcı düğmeye hiç dokunmadan evrak onaylanabilirdi.
 */

const ACTION_LABELS: Record<string, string> = {
  ONAYLA: 'Onayla',
  BASKANA_ILET: 'Başkana ilet',
  TEKRAR_GONDER: 'Tekrar gönder',
}

const STATUS_LABELS: Record<string, string> = {
  TASLAK: 'Taslak',
  BSK_YRD_INCELEMESINDE: 'Başkan Yardımcısı incelemesinde',
  BASKAN_INCELEMESINDE: 'Başkan incelemesinde',
  DUZENLEME_BEKLIYOR: 'Düzenleme bekliyor',
  ONAYLANDI: 'Onaylandı',
  REDDEDILDI: 'Reddedildi',
}

type PageState =
  | { kind: 'loading' }
  | { kind: 'ready'; preview: MailActionPreview }
  | { kind: 'done'; recordId: string }
  | { kind: 'error'; message: string }

/** Anahtarı adres parçasından okur ve parçayı adresten siler. */
function readTokenFromHash(): string {
  if (typeof window === 'undefined') return ''

  const hash = window.location.hash.startsWith('#') ? window.location.hash.slice(1) : window.location.hash
  const token = new URLSearchParams(hash).get('token')?.trim() ?? ''

  if (token) {
    window.history.replaceState(null, '', window.location.pathname + window.location.search)
  }
  return token
}

export function QuickActionPage() {
  const [state, setState] = useState<PageState>({ kind: 'loading' })
  const [submitting, setSubmitting] = useState(false)
  // Anahtar adresten silindiği için bileşen ömrü boyunca burada tutulur.
  const tokenRef = useRef<string>('')

  useEffect(() => {
    const token = readTokenFromHash()
    tokenRef.current = token

    if (!token) {
      setState({ kind: 'error', message: 'Bağlantı eksik veya bozuk görünüyor.' })
      return
    }

    let cancelled = false
    previewMailAction(token)
      .then((preview) => {
        if (!cancelled) setState({ kind: 'ready', preview })
      })
      .catch((error: unknown) => {
        if (!cancelled) setState({ kind: 'error', message: describeError(error) })
      })

    return () => {
      cancelled = true
    }
  }, [])

  const confirm = async () => {
    if (state.kind !== 'ready' || submitting) return

    setSubmitting(true)
    try {
      const { recordId } = await consumeMailAction(tokenRef.current)
      setState({ kind: 'done', recordId })
    } catch (error: unknown) {
      setState({ kind: 'error', message: describeError(error) })
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="relative flex min-h-screen items-center justify-center overflow-hidden bg-app-canvas px-4 py-8 sm:px-8">
      <div className="pointer-events-none absolute -right-40 -top-40 size-[30rem] rounded-full bg-brand-200/60 blur-3xl dark:bg-brand-900/45" />
      <div className="pointer-events-none absolute -bottom-40 -left-28 size-[28rem] rounded-full bg-blue-100/60 blur-3xl dark:bg-blue-900/40" />

      <section
        className="relative w-full max-w-xl rounded-[1.5rem] border border-app-border bg-app-surface p-6 shadow-2xl shadow-slate-900/[0.08] sm:p-9 lg:p-11"
        aria-labelledby="quick-action-title"
      >
        <Brand />

        {state.kind === 'loading' ? <LoadingView /> : null}
        {state.kind === 'ready' ? (
          <ConfirmView preview={state.preview} submitting={submitting} onConfirm={confirm} />
        ) : null}
        {state.kind === 'done' ? <DoneView recordId={state.recordId} /> : null}
        {state.kind === 'error' ? <ErrorView message={state.message} /> : null}
      </section>
    </main>
  )
}

function LoadingView() {
  return (
    <div className="mt-8 flex flex-col items-center gap-3 text-center" role="status">
      <Loader2 aria-hidden className="size-7 animate-spin text-brand-600" />
      <h1 id="quick-action-title" className="text-lg font-semibold text-app-strong">
        Bağlantı doğrulanıyor
      </h1>
      <p className="text-sm text-app-muted">Lütfen bekleyin.</p>
    </div>
  )
}

function ConfirmView({
  preview,
  submitting,
  onConfirm,
}: {
  preview: MailActionPreview
  submitting: boolean
  onConfirm: () => void
}) {
  const actionLabel = ACTION_LABELS[preview.action] ?? preview.action
  const statusLabel = preview.recordStatus ? STATUS_LABELS[preview.recordStatus] ?? preview.recordStatus : null

  return (
    <div className="mt-8">
      <h1 id="quick-action-title" className="text-xl font-semibold text-app-strong">
        İşlemi onaylayın
      </h1>
      <p className="mt-2 text-sm text-app-muted">
        Bu bağlantı tek kullanımlıktır. Onayladığınızda işlem sizin adınıza kaydedilir.
      </p>

      <dl className="mt-6 divide-y divide-app-border rounded-xl border border-app-border bg-app-canvas/60">
        <Row label="Evrak" value={preview.recordTitle ?? '—'} />
        {statusLabel ? <Row label="Güncel durum" value={statusLabel} /> : null}
        <Row label="Yapılacak işlem" value={actionLabel} />
        {preview.recipientName ? <Row label="İşlemi yapan" value={preview.recipientName} /> : null}
      </dl>

      <button
        type="button"
        onClick={onConfirm}
        disabled={submitting}
        className="mt-6 inline-flex w-full items-center justify-center gap-2 rounded-xl bg-brand-600 px-4 py-3 text-sm font-semibold text-white transition hover:bg-brand-700 disabled:cursor-not-allowed disabled:opacity-60"
      >
        {submitting ? <Loader2 aria-hidden className="size-4 animate-spin" /> : null}
        {submitting ? 'İşleniyor…' : actionLabel}
      </button>

      <p className="mt-4 text-center text-xs text-app-muted">
        İşlemi burada yapmak istemiyorsanız{' '}
        <Link className="font-semibold text-brand-700 hover:underline dark:text-brand-300" to="/giris">
          uygulamaya giriş yapabilirsiniz
        </Link>
        .
      </p>
    </div>
  )
}

function DoneView({ recordId }: { recordId: string }) {
  return (
    <div className="mt-8 flex flex-col items-center gap-3 text-center">
      <CheckCircle2 aria-hidden className="size-9 text-emerald-600" />
      <h1 id="quick-action-title" className="text-xl font-semibold text-app-strong">
        İşlem tamamlandı
      </h1>
      <p className="text-sm text-app-muted">Evrak bir sonraki aşamaya geçti.</p>
      <Link
        className="mt-2 inline-flex items-center gap-2 rounded-xl bg-brand-600 px-4 py-3 text-sm font-semibold text-white transition hover:bg-brand-700"
        to={`/kayitlar/${recordId}`}
      >
        Evrağı görüntüle
        <ArrowRight aria-hidden className="size-4" />
      </Link>
    </div>
  )
}

function ErrorView({ message }: { message: string }) {
  return (
    <div className="mt-8 flex flex-col items-center gap-3 text-center">
      <AlertTriangle aria-hidden className="size-9 text-amber-600" />
      <h1 id="quick-action-title" className="text-xl font-semibold text-app-strong">
        İşlem yapılamadı
      </h1>
      <p className="text-sm text-app-muted">{message}</p>
      <Link
        className="mt-2 inline-flex items-center gap-2 rounded-xl bg-brand-600 px-4 py-3 text-sm font-semibold text-white transition hover:bg-brand-700"
        to="/giris"
      >
        Uygulamaya giriş yap
        <ArrowRight aria-hidden className="size-4" />
      </Link>
    </div>
  )
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-start justify-between gap-4 px-4 py-3">
      <dt className="text-xs font-medium uppercase tracking-wide text-app-muted">{label}</dt>
      <dd className="text-right text-sm font-semibold text-app-strong">{value}</dd>
    </div>
  )
}

/**
 * Backend mesajı kullanıcıya olduğu gibi gösterilir: anahtar hataları zaten
 * ayrım yapmayan tek bir metin döndürür, workflow hataları ise evrağın arada
 * ilerlediğini anlatır. İkisi de kullanıcının görmesi gereken bilgidir.
 */
function describeError(error: unknown): string {
  if (error instanceof ApiClientError && error.message) return error.message
  return 'Bağlantı doğrulanamadı. Lütfen uygulamaya giriş yaparak deneyin.'
}
