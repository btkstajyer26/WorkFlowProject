import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useRef, useState } from 'react'
import { useForm } from 'react-hook-form'
import { AlertCircle, ArrowLeft, Save, Trash2, X } from 'lucide-react'
import { Navigate, useNavigate, useParams } from 'react-router'
import { ApiClientError } from '../api/errors'
import { deleteRecordDraft, getRecordDetail, updateRecordDraft } from '../api/recordDetails'
import { CategoryLoadError } from '../components/records/CategoryLoadError'
import { RecordStatusBadge } from '../components/records/RecordStatusBadge'
import { RecordFilesPanel } from '../components/records/RecordFilesPanel'
import { maxRecordTitleLength } from '../config/records'
import { useCategories } from '../context/categoryState'
import { useModalDialog } from '../hooks/useModalDialog'
import { queryKeys } from '../query/queryKeys'
import { recordFormSchema, type RecordFormValues } from '../schemas/record'
import type { UserRole } from '../types/auth'
import type { WorkflowRecord } from '../types/record'

const fieldClass =
  'w-full rounded-xl border border-app-border bg-app-surface px-3.5 py-3 text-sm text-app-text-strong outline-none transition placeholder:text-app-text-faint focus:border-brand-400 focus:ring-4 focus:ring-brand-100 dark:focus:ring-brand-800/60'

export function BackendRecordEditPage({ role }: { role: UserRole }) {
  const { recordId } = useParams()
  const { categories, status: categoryStatus, reloadCategories } = useCategories()
  const categoryRevision = categories.map((category) => `${category.id}:${category.name}`).join('|')
  const recordQuery = useQuery({
    queryKey: queryKeys.records.detail(recordId ?? 'missing', categoryRevision),
    queryFn: () => getRecordDetail(recordId!, categories),
    enabled: Boolean(recordId) && categoryStatus === 'ready',
  })

  if (!recordId) return <Navigate to="/404" replace />
  if (categoryStatus === 'error') {
    return (
      <section className="rounded-xl border border-rose-200 bg-rose-50 px-5 py-6 text-center dark:border-rose-900/70 dark:bg-rose-950/40">
        <h1 className="font-bold text-rose-900 dark:text-rose-100">Kategoriler yüklenemedi</h1>
        <button type="button" onClick={reloadCategories} className="mt-4 rounded-lg border border-rose-300 px-4 py-2 text-sm font-bold text-rose-800 dark:border-rose-800 dark:text-rose-200">
          Tekrar dene
        </button>
      </section>
    )
  }
  if (recordQuery.error instanceof ApiClientError && recordQuery.error.status === 403) {
    return <Navigate to="/403" replace />
  }
  if (recordQuery.error instanceof ApiClientError && recordQuery.error.status === 404) {
    return <Navigate to="/404" replace />
  }

  if (recordQuery.isPending) {
    return <p className="rounded-xl border border-app-border bg-app-surface px-5 py-8 text-center text-sm text-app-text-muted" role="status">Kayıt yükleniyor…</p>
  }

  if (recordQuery.isError || !recordQuery.data) {
    return (
      <section className="rounded-xl border border-rose-200 bg-rose-50 px-5 py-6 text-center dark:border-rose-900/70 dark:bg-rose-950/40">
        <h1 className="font-bold text-rose-900 dark:text-rose-100">Kayıt yüklenemedi</h1>
        <p className="mt-2 text-sm text-rose-800 dark:text-rose-200">
          {recordQuery.error instanceof Error ? recordQuery.error.message : 'Beklenmeyen bir hata oluştu.'}
        </p>
        <button type="button" onClick={() => recordQuery.refetch()} className="mt-4 rounded-lg border border-rose-300 px-4 py-2 text-sm font-bold text-rose-800 dark:border-rose-800 dark:text-rose-200">
          Tekrar dene
        </button>
      </section>
    )
  }

  const editable = role === 'CALISAN' && (recordQuery.data.status === 'TASLAK' || recordQuery.data.status === 'DUZENLEME_BEKLIYOR')
  if (!editable) return <Navigate to="/403" replace />

  return <BackendEditableRecordForm key={recordQuery.data.id} record={recordQuery.data} />
}

function BackendEditableRecordForm({ record }: { record: WorkflowRecord }) {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { categories, status: categoryStatus, reloadCategories } = useCategories()
  const [activeDialog, setActiveDialog] = useState<'delete' | 'discard' | null>(null)
  const [feedback, setFeedback] = useState<string | null>(null)
  const dialogRef = useRef<HTMLElement>(null)
  const dialogCloseButtonRef = useRef<HTMLButtonElement>(null)
  const {
    register,
    handleSubmit,
    watch,
    reset,
    formState: { errors, isDirty },
  } = useForm<RecordFormValues>({
    resolver: zodResolver(recordFormSchema),
    defaultValues: {
      title: record.title,
      categoryId: record.categoryId,
      description: record.description,
    },
  })
  const titleValue = watch('title')
  const latestRevisionNote = record.status === 'DUZENLEME_BEKLIYOR'
    ? record.history.at(-1)?.note?.trim()
    : undefined

  useModalDialog({
    open: Boolean(activeDialog),
    onClose: () => setActiveDialog(null),
    dialogRef,
    initialFocusRef: dialogCloseButtonRef,
  })

  useEffect(() => {
    if (!isDirty) return
    const handleBeforeUnload = (event: BeforeUnloadEvent) => event.preventDefault()
    window.addEventListener('beforeunload', handleBeforeUnload)
    return () => window.removeEventListener('beforeunload', handleBeforeUnload)
  }, [isDirty])

  const updateMutation = useMutation({
    mutationFn: (values: RecordFormValues) => updateRecordDraft(record.id, values),
    onSuccess: async (_recordId, values) => {
      setFeedback('Değişiklikler veritabanına kaydedildi.')
      reset(values)
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: queryKeys.records.detail(record.id) }),
        queryClient.invalidateQueries({ queryKey: queryKeys.records.lists() }),
      ])
    },
  })
  const deleteMutation = useMutation({
    mutationFn: () => deleteRecordDraft(record.id),
    onSuccess: async () => {
      queryClient.removeQueries({ queryKey: queryKeys.records.detail(record.id) })
      await queryClient.invalidateQueries({ queryKey: queryKeys.records.lists() })
      navigate('/kayitlar?gorunum=taslaklar', { replace: true })
    },
  })
  const mutationError = updateMutation.error ?? deleteMutation.error
  const mutationBusy = updateMutation.isPending || deleteMutation.isPending

  return (
    <div className="space-y-5">
      <button
        type="button"
        onClick={() => {
          if (isDirty) setActiveDialog('discard')
          else navigate(`/kayitlar/${record.id}`)
        }}
        className="inline-flex items-center gap-2 text-sm font-bold text-app-text-muted transition hover:text-brand-700 dark:hover:text-brand-300 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
      >
        <ArrowLeft className="size-4" aria-hidden="true" />
        Kayıt detayına dön
      </button>

      <header className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-app-text sm:text-3xl">
            {record.status === 'TASLAK' ? 'Taslağı Düzenle' : 'Düzeltmeleri Tamamla'}
          </h1>
          <p className="mt-2 text-sm text-app-text-muted">Bu ekrandaki kayıt işlemleri doğrudan veritabanına uygulanır.</p>
        </div>
        <RecordStatusBadge status={record.status} />
      </header>

      <form className="grid gap-5 xl:grid-cols-[minmax(0,1fr)_340px]" noValidate onSubmit={handleSubmit((values) => updateMutation.mutate(values))}>
        <section className="rounded-2xl border border-app-border bg-app-surface p-5 shadow-sm sm:p-6">
          <div className="space-y-5">
            <label className="block" htmlFor="edit-record-title">
              <span className="mb-1.5 flex items-center justify-between gap-3 text-xs font-bold text-app-text-secondary">
                <span>Başlık *</span>
                <span className="text-[11px] font-semibold text-app-text-faint">{titleValue.length} / {maxRecordTitleLength}</span>
              </span>
              <input id="edit-record-title" {...register('title')} maxLength={maxRecordTitleLength} aria-invalid={Boolean(errors.title)} aria-describedby={errors.title ? 'edit-record-title-error' : undefined} className={fieldClass} />
              {errors.title ? <FieldError id="edit-record-title-error" message={errors.title.message} /> : null}
            </label>

            <div>
              <label htmlFor="edit-record-category" className="mb-1.5 block text-xs font-bold text-app-text-secondary">Kategori *</label>
              <select
                id="edit-record-category"
                {...register('categoryId', { valueAsNumber: true })}
                disabled={categoryStatus !== 'ready'}
                aria-invalid={Boolean(errors.categoryId)}
                aria-describedby={errors.categoryId ? 'edit-record-category-error' : categoryStatus === 'error' ? 'edit-record-category-load-error' : undefined}
                className={`${fieldClass} disabled:cursor-wait disabled:bg-app-surface-strong`}
              >
                {categoryStatus === 'ready'
                  ? categories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)
                  : <option value={record.categoryId}>{record.category}</option>}
              </select>
              {errors.categoryId ? <FieldError id="edit-record-category-error" message={errors.categoryId.message} /> : null}
              {categoryStatus === 'error' ? <CategoryLoadError id="edit-record-category-load-error" onRetry={reloadCategories} /> : null}
            </div>

            <label className="block" htmlFor="edit-record-description">
              <span className="mb-1.5 block text-xs font-bold text-app-text-secondary">Kayıt açıklaması *</span>
              <textarea id="edit-record-description" {...register('description')} rows={12} aria-invalid={Boolean(errors.description)} aria-describedby={errors.description ? 'edit-record-description-error' : undefined} className={`${fieldClass} min-h-64 resize-y leading-6`} />
              {errors.description ? <FieldError id="edit-record-description-error" message={errors.description.message} /> : null}
            </label>

            <RecordFilesPanel recordId={record.id} editable />
          </div>
        </section>

        <aside className="space-y-4 xl:sticky xl:top-6 xl:self-start">
          {latestRevisionNote ? (
            <section className="rounded-2xl border border-amber-200 bg-amber-50 p-5 dark:border-amber-800/70 dark:bg-amber-950/40">
              <div className="flex items-start gap-3">
                <AlertCircle className="mt-0.5 size-5 shrink-0 text-amber-700 dark:text-amber-300" aria-hidden="true" />
                <div>
                  <h2 className="text-sm font-bold text-amber-950 dark:text-amber-100">Düzeltme Talebi</h2>
                  <p className="mt-2 text-sm leading-6 text-amber-900 dark:text-amber-200">{latestRevisionNote}</p>
                </div>
              </div>
            </section>
          ) : null}

          <section className="rounded-2xl border border-app-border bg-app-surface p-5 shadow-sm">
            <h2 className="font-bold text-app-text">Taslağı Kaydet</h2>
            <p className="mt-2 text-xs leading-5 text-app-text-muted">Kaydettiğiniz taslağı kayıt detayından incelemeye gönderebilirsiniz.</p>
            {feedback ? <p className="mt-4 rounded-xl bg-emerald-50 px-3 py-2.5 text-xs font-semibold text-emerald-800 dark:bg-emerald-950/40 dark:text-emerald-200" role="status">{feedback}</p> : null}
            {mutationError ? <p className="mt-4 rounded-xl bg-rose-50 px-3 py-2.5 text-xs font-semibold text-rose-800 dark:bg-rose-950/40 dark:text-rose-200" role="alert">{mutationError instanceof Error ? mutationError.message : 'İşlem tamamlanamadı.'}</p> : null}
            <div className="mt-5 grid gap-2">
              <button type="submit" disabled={mutationBusy || categoryStatus !== 'ready'} className="flex min-h-11 items-center justify-center gap-2 rounded-xl bg-brand-700 px-4 text-sm font-bold text-white transition hover:bg-brand-800 disabled:cursor-not-allowed disabled:opacity-60">
                <Save className="size-4" aria-hidden="true" />
                Taslağı Kaydet
              </button>
              {record.status === 'TASLAK' ? (
                <button type="button" onClick={() => setActiveDialog('delete')} disabled={mutationBusy} className="mt-2 flex min-h-11 items-center justify-center gap-2 border-t border-app-border-subtle pt-3 text-sm font-bold text-rose-600 transition hover:text-rose-700 disabled:opacity-60 dark:text-rose-400 dark:hover:text-rose-300">
                  <Trash2 className="size-4" aria-hidden="true" />
                  Taslağı Sil
                </button>
              ) : null}
            </div>
          </section>
        </aside>
      </form>

      {activeDialog ? (
        <div className="fixed inset-0 z-[80] flex items-end justify-center bg-slate-950/35 backdrop-blur-[2px] sm:items-center sm:p-4" role="presentation">
          <section ref={dialogRef} tabIndex={-1} role="dialog" aria-modal="true" aria-labelledby="backend-record-dialog-title" className="w-full rounded-t-3xl bg-app-surface p-5 shadow-2xl sm:max-w-md sm:rounded-2xl sm:p-6">
            <div className="flex items-start gap-3">
              <div className="min-w-0 flex-1">
                <h2 id="backend-record-dialog-title" className="text-lg font-bold text-app-text">
                  {activeDialog === 'delete' ? 'Taslak silinsin mi?' : 'Kaydedilmemiş değişiklikler silinsin mi?'}
                </h2>
                <p className="mt-2 text-sm leading-6 text-app-text-muted">
                  {activeDialog === 'delete' ? 'Bu işlem geri alınamaz.' : 'Kayıt detayına dönerseniz formdaki değişiklikler kaybolacak.'}
                </p>
              </div>
              <button ref={dialogCloseButtonRef} type="button" onClick={() => setActiveDialog(null)} className="flex size-10 shrink-0 items-center justify-center rounded-xl text-app-text-subtle hover:bg-app-surface-strong" aria-label="Pencereyi kapat">
                <X className="size-5" aria-hidden="true" />
              </button>
            </div>
            <div className="mt-6 grid grid-cols-2 gap-3">
              <button type="button" onClick={() => setActiveDialog(null)} className="min-h-11 rounded-xl border border-app-border px-4 text-sm font-bold text-app-text-secondary hover:bg-app-surface-muted">İptal</button>
              <button
                type="button"
                disabled={mutationBusy}
                onClick={() => {
                  const action = activeDialog
                  setActiveDialog(null)
                  if (action === 'delete') deleteMutation.mutate()
                  else navigate(`/kayitlar/${record.id}`)
                }}
                className="min-h-11 rounded-xl bg-rose-600 px-4 text-sm font-bold text-white transition hover:bg-rose-700 disabled:opacity-60"
              >
                {activeDialog === 'delete' ? 'Evet, Taslağı Sil' : 'Değişiklikleri Sil'}
              </button>
            </div>
          </section>
        </div>
      ) : null}
    </div>
  )
}

function FieldError({ id, message }: { id: string; message?: string }) {
  return <span id={id} className="mt-1.5 block text-xs font-semibold text-rose-700 dark:text-rose-300" role="alert">{message}</span>
}
