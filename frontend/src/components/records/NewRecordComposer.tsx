import { useEffect, useRef, useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { useNavigate } from 'react-router'
import {
  ChevronDown,
  CircleCheck,
  FilePlus2,
  FileText,
  Maximize2,
  Minimize2,
  Paperclip,
  Save,
  Send,
  Trash2,
  X,
} from 'lucide-react'
import {
  attachmentAcceptValue,
  getAttachmentValidationError,
  maxRecordTitleLength,
} from '../../config/records'
import { useCategories } from '../../context/categoryState'
import { recordFormSchema, type RecordFormValues } from '../../schemas/record'
import { useModalDialog } from '../../hooks/useModalDialog'
import { useSingleFlight } from '../../hooks/useSingleFlight'
import { CategoryLoadError } from './CategoryLoadError'
import { createRecordDraft, updateRecordDraft } from '../../api/recordDetails'
import { performWorkflowAction } from '../../api/workflow'
import { uploadRecordFile } from '../../api/files'
import { queryKeys } from '../../query/queryKeys'

type NewRecordComposerProps = {
  open: boolean
  requestId: number
  onClose: () => void
}

const fieldClass =
  'w-full rounded-xl border border-app-border bg-app-surface px-3.5 py-3 text-sm text-app-text-strong outline-none transition placeholder:text-app-text-faint focus:border-brand-400 focus:ring-4 focus:ring-brand-100 dark:focus:ring-brand-800/60'

const emptyRecordFormValues: RecordFormValues = {
  title: '',
  categoryId: 0,
  description: '',
}

export function NewRecordComposer({ open, requestId, onClose }: NewRecordComposerProps) {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { categories, status: categoryStatus, reloadCategories } = useCategories()
  const [minimized, setMinimized] = useState(false)
  const [expanded, setExpanded] = useState(false)
  const [attachments, setAttachments] = useState<File[]>([])
  const [attachmentsDirty, setAttachmentsDirty] = useState(false)
  const [attachmentError, setAttachmentError] = useState<string | null>(null)
  const [attachmentDragActive, setAttachmentDragActive] = useState(false)
  const [feedback, setFeedback] = useState<string | null>(null)
  const [draftSaved, setDraftSaved] = useState(false)
  const [draftId, setDraftId] = useState<string | null>(null)
  const [attentionTarget, setAttentionTarget] = useState<'save' | 'new' | null>(null)
  const [attentionVersion, setAttentionVersion] = useState(0)
  const [discardDialogOpen, setDiscardDialogOpen] = useState(false)
  const titleInputRef = useRef<HTMLInputElement>(null)
  const saveButtonRef = useRef<HTMLButtonElement>(null)
  const newRecordButtonRef = useRef<HTMLButtonElement>(null)
  const attentionTimerRef = useRef<number | null>(null)
  const discardDialogRef = useRef<HTMLElement>(null)
  const continueEditingRef = useRef<HTMLButtonElement>(null)
  const attachmentDragDepthRef = useRef(0)
  const wasOpenRef = useRef(false)
  const lastRequestIdRef = useRef(requestId)
  const minimizedRef = useRef(minimized)
  const draftSavedRef = useRef(draftSaved)
  const {
    register,
    handleSubmit,
    reset,
    watch,
    formState: { errors, isDirty },
  } = useForm<RecordFormValues>({
    resolver: zodResolver(recordFormSchema),
    defaultValues: emptyRecordFormValues,
  })
  const titleValue = watch('title')
  const { busy: mutationBusy, run: runMutation } = useSingleFlight()
  const saveRecordMutation = useMutation({
    mutationFn: ({ recordId, values }: { recordId: string | null; values: RecordFormValues }) => (
      recordId
        ? updateRecordDraft(recordId, values)
        : createRecordDraft(values)
    ),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: queryKeys.records.all })
    },
  })
  const submitRecordMutation = useMutation({
    mutationFn: ({ recordId }: { recordId: string }) => performWorkflowAction(recordId, { action: 'GONDER' }),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: queryKeys.records.all }),
        queryClient.invalidateQueries({ queryKey: queryKeys.notifications.all }),
      ])
    },
  })
  const uploadAttachmentsMutation = useMutation({
    mutationFn: async ({ recordId, files }: { recordId: string; files: File[] }) => {
      for (const file of files) {
        await uploadRecordFile(recordId, file)
        setAttachments((current) => current.filter((candidate) => candidate !== file))
      }
      return files.length
    },
    onSuccess: async (_count, { recordId }) => {
      await queryClient.invalidateQueries({ queryKey: queryKeys.records.files(recordId) })
    },
  })
  const recordMutationBusy = mutationBusy || saveRecordMutation.isPending || submitRecordMutation.isPending || uploadAttachmentsMutation.isPending
  const recordMutationError = uploadAttachmentsMutation.error ?? submitRecordMutation.error ?? saveRecordMutation.error
  const hasUnsavedChanges = !draftSaved && (isDirty || attachmentsDirty)
  useModalDialog({
    open: open && discardDialogOpen,
    onClose: () => setDiscardDialogOpen(false),
    dialogRef: discardDialogRef,
    initialFocusRef: continueEditingRef,
  })
  const { ref: titleFieldRef, ...titleField } = register('title')

  minimizedRef.current = minimized
  draftSavedRef.current = draftSaved

  useEffect(() => {
    if (!open || minimized || discardDialogOpen) return

    titleInputRef.current?.focus()

    const handleEscape = (event: KeyboardEvent) => {
      if (event.key !== 'Escape' || discardDialogOpen) return
      if (hasUnsavedChanges) setDiscardDialogOpen(true)
      else onClose()
    }

    window.addEventListener('keydown', handleEscape)
    return () => window.removeEventListener('keydown', handleEscape)
  }, [discardDialogOpen, hasUnsavedChanges, minimized, onClose, open])

  useEffect(() => {
    if (!open || !hasUnsavedChanges) return
    const handleBeforeUnload = (event: BeforeUnloadEvent) => event.preventDefault()
    window.addEventListener('beforeunload', handleBeforeUnload)
    return () => window.removeEventListener('beforeunload', handleBeforeUnload)
  }, [hasUnsavedChanges, open])

  useEffect(() => {
    if (open) return
    setMinimized(false)
    setExpanded(false)
    setAttachments([])
    setAttachmentsDirty(false)
    setAttachmentError(null)
    setAttachmentDragActive(false)
    attachmentDragDepthRef.current = 0
    setFeedback(null)
    setDraftSaved(false)
    setDraftId(null)
    setAttentionTarget(null)
    setDiscardDialogOpen(false)
    reset(emptyRecordFormValues)
  }, [open, reset])

  useEffect(() => {
    const wasOpen = wasOpenRef.current
    wasOpenRef.current = open

    if (!open) {
      lastRequestIdRef.current = requestId
      return
    }

    if (!wasOpen) {
      lastRequestIdRef.current = requestId
      return
    }

    if (lastRequestIdRef.current === requestId) return
    lastRequestIdRef.current = requestId

    if (minimizedRef.current) setMinimized(false)

    const target = draftSavedRef.current ? 'new' : 'save'
    setAttentionTarget(target)
    setAttentionVersion((current) => current + 1)

    if (attentionTimerRef.current !== null) window.clearTimeout(attentionTimerRef.current)
    attentionTimerRef.current = window.setTimeout(() => setAttentionTarget(null), 5000)
  }, [open, requestId])

  useEffect(() => {
    if (!attentionTarget) return

    const frameId = window.requestAnimationFrame(() => {
      const targetButton = attentionTarget === 'save' ? saveButtonRef.current : newRecordButtonRef.current
      targetButton?.focus()
    })

    return () => window.cancelAnimationFrame(frameId)
  }, [attentionTarget, attentionVersion])

  useEffect(
    () => () => {
      if (attentionTimerRef.current !== null) window.clearTimeout(attentionTimerRef.current)
    },
    [],
  )

  const startNewRecord = () => {
    reset(emptyRecordFormValues)
    setAttachments([])
    setAttachmentsDirty(false)
    setAttachmentError(null)
    setAttachmentDragActive(false)
    attachmentDragDepthRef.current = 0
    setFeedback(null)
    setDraftSaved(false)
    setDraftId(null)
    setAttentionTarget(null)
    titleInputRef.current?.focus()
  }

  const requestClose = () => {
    if (hasUnsavedChanges) setDiscardDialogOpen(true)
    else onClose()
  }

  const addAttachments = (selectedFiles: File[]) => {
    if (selectedFiles.length === 0) return

    const validationError = getAttachmentValidationError(selectedFiles)
    if (validationError) {
      setAttachmentError(validationError)
      return
    }

    const seenFiles = new Set(
      attachments.map((file) => `${file.name}\u0000${file.size}\u0000${file.lastModified}`),
    )
    const uniqueFiles = selectedFiles.filter((file) => {
      const fileKey = `${file.name}\u0000${file.size}\u0000${file.lastModified}`
      if (seenFiles.has(fileKey)) return false
      seenFiles.add(fileKey)
      return true
    })

    setAttachmentError(
      uniqueFiles.length === selectedFiles.length
        ? null
        : 'Aynı dosya birden fazla kez eklenemez.',
    )
    if (uniqueFiles.length === 0) return

    setAttachmentsDirty(true)
    setDraftSaved(false)
    setAttachments((current) => [...current, ...uniqueFiles])
  }

  const saveDraft = handleSubmit((values) => runMutation(async () => {
    try {
      let uploadedFileCount = 0
      const savedRecordId = await saveRecordMutation.mutateAsync({ recordId: draftId, values })
      setDraftId(savedRecordId)
      if (attachments.length > 0) {
        uploadedFileCount = await uploadAttachmentsMutation.mutateAsync({ recordId: savedRecordId, files: [...attachments] })
      }
      setFeedback(uploadedFileCount > 0 ? `${uploadedFileCount} ek dosya yüklendi.` : null)
      setDraftSaved(true)
      setAttachmentsDirty(false)
      setAttentionTarget(null)
      reset(values)
    } catch {
      // Mutation hatası formun altında gösterilir; girilen değerler korunur.
    }
  }))

  const submitRecord = handleSubmit((values) => runMutation(async () => {
    try {
      const savedRecordId = await saveRecordMutation.mutateAsync({ recordId: draftId, values })
      setDraftId(savedRecordId)
      if (attachments.length > 0) {
        await uploadAttachmentsMutation.mutateAsync({ recordId: savedRecordId, files: [...attachments] })
      }
      setAttachmentsDirty(false)
      setDraftSaved(true)
      reset(values)
      await submitRecordMutation.mutateAsync({ recordId: savedRecordId })
      onClose()
      navigate(`/kayitlar/${savedRecordId}`)
    } catch {
      // Kayit basarili, workflow basarisiz olabilir. draftId korunur ve hata formda gosterilir.
    }
  }))

  if (!open) return null

  if (minimized) {
    return (
      <div className="fixed inset-x-3 bottom-3 z-[60] ml-auto sm:right-5 sm:left-auto sm:w-80 lg:right-8">
        <button
          type="button"
          onClick={() => setMinimized(false)}
          className="flex min-h-14 w-full items-center gap-3 rounded-2xl border border-brand-200 dark:border-brand-700/60 bg-app-surface px-4 text-left shadow-2xl shadow-slate-900/15 transition hover:-translate-y-0.5 hover:border-brand-300 dark:hover:border-brand-600 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
        >
          <span className="flex size-9 items-center justify-center rounded-xl bg-brand-100 dark:bg-brand-900/45 text-brand-700 dark:text-brand-300">
            <FileText className="size-4.5" aria-hidden="true" />
          </span>
          <span className="min-w-0 flex-1">
            <span className="block text-sm font-bold text-app-text-strong">Yeni Kayıt</span>
            <span className="block truncate text-xs text-app-text-subtle">Taslak düzenleniyor</span>
          </span>
          <Maximize2 className="size-4 text-app-text-faint" aria-hidden="true" />
        </button>
      </div>
    )
  }

  return (
    <div
      className={`composer-genie-enter fixed z-[60] transition-all duration-300 max-sm:inset-0 ${
        expanded
          ? 'inset-3 sm:inset-6 lg:left-[19.5rem]'
          : 'inset-x-0 bottom-0 top-0 sm:inset-auto sm:bottom-5 sm:right-5 sm:h-[min(720px,calc(100vh-2.5rem))] sm:w-[min(620px,calc(100vw-2.5rem))] lg:right-8'
      }`}
      role="dialog"
      aria-labelledby="new-record-title"
    >
      <form
        className={`flex h-full min-h-0 flex-col overflow-hidden bg-app-surface shadow-2xl shadow-slate-950/20 ring-1 ring-app-border ${
          expanded ? 'rounded-2xl' : 'sm:rounded-2xl'
        }`}
        noValidate
        onChange={() => {
          if (draftSaved) setDraftSaved(false)
          if (attentionTarget) setAttentionTarget(null)
        }}
        onSubmit={submitRecord}
      >
        <header className="flex min-h-16 items-center gap-3 border-b border-app-border bg-app-surface-muted/80 px-4 sm:px-5">
          <span className="flex size-9 shrink-0 items-center justify-center rounded-xl bg-brand-100 dark:bg-brand-900/45 text-brand-700 dark:text-brand-300">
            <FileText className="size-4.5" aria-hidden="true" />
          </span>
          <div className="min-w-0 flex-1">
            <h2 id="new-record-title" className="text-sm font-bold text-app-text sm:text-base">
              Yeni Kayıt
            </h2>
            <p className={`truncate text-xs ${draftSaved ? 'font-semibold text-emerald-700 dark:text-emerald-300' : 'text-app-text-subtle'}`}>
              {draftSaved ? 'Taslak kaydedildi' : 'Henüz kaydedilmedi'}
            </p>
          </div>
          <div className="flex items-center gap-0.5">
            <button
              type="button"
              onClick={() => setMinimized(true)}
              className="hidden size-10 items-center justify-center rounded-xl text-app-text-subtle transition hover:bg-app-surface hover:text-app-text-strong focus-visible:outline-2 focus-visible:outline-brand-500 sm:flex"
              aria-label="Formu küçült"
            >
              <Minimize2 className="size-4" aria-hidden="true" />
            </button>
            <button
              type="button"
              onClick={() => setExpanded((current) => !current)}
              className="hidden size-10 items-center justify-center rounded-xl text-app-text-subtle transition hover:bg-app-surface hover:text-app-text-strong focus-visible:outline-2 focus-visible:outline-brand-500 sm:flex"
              aria-label={expanded ? 'Pencere görünümüne dön' : 'Formu büyüt'}
            >
              {expanded ? (
                <Minimize2 className="size-4" aria-hidden="true" />
              ) : (
                <Maximize2 className="size-4" aria-hidden="true" />
              )}
            </button>
            <button
              type="button"
              onClick={requestClose}
              className="flex size-10 items-center justify-center rounded-xl text-app-text-subtle transition hover:bg-app-surface hover:text-rose-600 dark:hover:text-rose-400 focus-visible:outline-2 focus-visible:outline-brand-500"
              aria-label="Yeni kayıt formunu kapat"
            >
              <X className="size-5" aria-hidden="true" />
            </button>
          </div>
        </header>

        <div className="min-h-0 flex-1 overflow-y-auto px-4 py-5 sm:px-6">
          <div className="space-y-5">
            <div>
              <div className="mb-1.5 flex items-center justify-between gap-3">
                <label htmlFor="record-title" className="block text-xs font-bold text-app-text-secondary">
                  Başlık <span className="text-rose-500">*</span>
                </label>
                <span className="text-[11px] font-semibold text-app-text-faint">
                  {titleValue.length} / {maxRecordTitleLength}
                </span>
              </div>
              <input
                id="record-title"
                {...titleField}
                ref={(element) => {
                  titleFieldRef(element)
                  titleInputRef.current = element
                }}
                maxLength={maxRecordTitleLength}
                placeholder="Örn. Yazılım lisansı talebi"
                aria-invalid={Boolean(errors.title)}
                aria-describedby={errors.title ? 'record-title-error' : undefined}
                className={fieldClass}
              />
              {errors.title ? <FieldError id="record-title-error" message={errors.title.message} /> : null}
            </div>

            <div>
              <label htmlFor="record-category" className="mb-1.5 block text-xs font-bold text-app-text-secondary">
                Kategori <span className="text-rose-500">*</span>
              </label>
              <div className="relative">
                <select
                  id="record-category"
                  {...register('categoryId', { valueAsNumber: true })}
                  disabled={categoryStatus !== 'ready'}
                  aria-invalid={Boolean(errors.categoryId)}
                  aria-describedby={errors.categoryId
                    ? 'record-category-error'
                    : categoryStatus === 'error' ? 'record-category-load-error' : undefined}
                  className={`${fieldClass} appearance-none pr-10 disabled:cursor-wait disabled:bg-app-surface-strong`}
                >
                  <option value={0}>
                    {categoryStatus === 'loading' ? 'Kategoriler yükleniyor…' : 'Kaydın kategorisini seçin'}
                  </option>
                  {categories.map((category) => (
                    <option key={category.id} value={category.id}>
                      {category.name}
                    </option>
                  ))}
                </select>
                <ChevronDown className="pointer-events-none absolute right-3.5 top-1/2 size-4 -translate-y-1/2 text-app-text-faint" aria-hidden="true" />
              </div>
              {errors.categoryId ? <FieldError id="record-category-error" message={errors.categoryId.message} /> : null}
              {categoryStatus === 'error' ? (
                <CategoryLoadError id="record-category-load-error" onRetry={reloadCategories} />
              ) : null}
            </div>

            <div>
              <label htmlFor="record-description" className="mb-1.5 block text-xs font-bold text-app-text-secondary">
                Açıklama <span className="text-rose-500">*</span>
              </label>
              <textarea
                id="record-description"
                {...register('description')}
                rows={expanded ? 12 : 8}
                placeholder="Talebinizi ve gerekli ayrıntıları yazın..."
                aria-invalid={Boolean(errors.description)}
                aria-describedby={errors.description ? 'record-description-error' : undefined}
                className={`${fieldClass} min-h-44 resize-none leading-6`}
              />
              {errors.description ? <FieldError id="record-description-error" message={errors.description.message} /> : null}
            </div>

            <div>
              <div className="mb-2 flex flex-col items-start gap-2 md:flex-row md:items-center md:gap-3">
                <div>
                  <p className="text-xs font-bold text-app-text-secondary">Ek dosyalar</p>
                  <p id="record-attachment-help" className="mt-0.5 text-xs text-app-text-subtle">
                    PDF (.pdf), Word (.docx, .doc), Excel (.xlsx, .xls), Resim (.png, .jpeg, .jpg) · en fazla 10 MB
                  </p>
                </div>
                <label
                  className="w-30 flex min-h-10 cursor-pointer items-center gap-2 rounded-xl border border-app-border bg-app-surface px-3 text-xs font-bold text-app-text-secondary transition hover:border-brand-300 hover:text-brand-700 focus-within:outline-2 focus-within:outline-brand-500 dark:hover:border-brand-600 dark:hover:text-brand-300"
                >
                  <Paperclip className="size-4" aria-hidden="true" />
                  Dosya ekle
                  <input
                    type="file"
                    multiple
                    accept={attachmentAcceptValue}
                    aria-invalid={Boolean(attachmentError)}
                    aria-describedby={`record-attachment-help${attachmentError ? ' record-attachment-error' : ''}`}
                    className="sr-only"
                    onChange={(event) => {
                      addAttachments(Array.from(event.target.files ?? []))
                      event.target.value = ''
                    }}
                  />
                </label>
              </div>

              {attachmentError ? <FieldError id="record-attachment-error" message={attachmentError} /> : null}

              <div
                role="group"
                aria-label="Ek dosya yükleme alanı"
                aria-describedby={`record-attachment-help${attachmentError ? ' record-attachment-error' : ''}`}
                className="relative rounded-xl"
                onDragEnter={(event) => {
                  if (!Array.from(event.dataTransfer.types).includes('Files')) return
                  event.preventDefault()
                  attachmentDragDepthRef.current += 1
                  setAttachmentDragActive(true)
                }}
                onDragOver={(event) => {
                  if (!Array.from(event.dataTransfer.types).includes('Files')) return
                  event.preventDefault()
                  event.dataTransfer.dropEffect = 'copy'
                }}
                onDragLeave={(event) => {
                  event.preventDefault()
                  attachmentDragDepthRef.current = Math.max(0, attachmentDragDepthRef.current - 1)
                  if (attachmentDragDepthRef.current === 0) setAttachmentDragActive(false)
                }}
                onDrop={(event) => {
                  event.preventDefault()
                  attachmentDragDepthRef.current = 0
                  setAttachmentDragActive(false)
                  addAttachments(Array.from(event.dataTransfer.files))
                }}
              >
                {attachments.length > 0 ? (
                  <ul className="space-y-2" aria-label="Eklenen dosyalar">
                    {attachments.map((attachment, index) => (
                      <li key={`${attachment.name}-${attachment.size}-${attachment.lastModified}`} className="flex items-center gap-3 rounded-xl bg-app-surface-muted px-3 py-2.5">
                        <span className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-app-surface text-brand-600 dark:text-brand-400 ring-1 ring-app-border">
                          <FileText className="size-4" aria-hidden="true" />
                        </span>
                        <span className="min-w-0 flex-1 truncate text-xs font-semibold text-app-text-secondary">{attachment.name}</span>
                        <button
                          type="button"
                          onClick={() => {
                            setDraftSaved(false)
                            setAttachmentsDirty(true)
                            setAttachments((current) => current.filter((_, itemIndex) => itemIndex !== index))
                          }}
                          className="flex size-8 items-center justify-center rounded-lg text-app-text-faint transition hover:bg-app-surface hover:text-rose-600 dark:hover:text-rose-400 focus-visible:outline-2 focus-visible:outline-brand-500"
                          aria-label={`${attachment.name} dosyasını kaldır`}
                        >
                          <Trash2 className="size-4" aria-hidden="true" />
                        </button>
                      </li>
                    ))}
                  </ul>
                ) : (
                  <div className="flex min-h-14 items-center justify-center gap-2 rounded-xl border border-dashed border-app-border bg-app-surface-muted/70 px-4 py-4 text-center text-xs text-app-text-subtle">
                    <FilePlus2 className="size-4 shrink-0" aria-hidden="true" />
                    Dosyaları buraya sürükleyip bırakın.
                  </div>
                )}

                {attachmentDragActive ? (
                  <div className="pointer-events-none absolute inset-0 z-10 flex items-center justify-center gap-2 rounded-xl border-2 border-dashed border-white/80 bg-brand-700/95 px-4 text-sm font-extrabold text-white">
                    <FilePlus2 className="size-5" aria-hidden="true" />
                    Dosyaları bırakın
                  </div>
                ) : null}
              </div>
            </div>

            {recordMutationError ? (
              <p className="rounded-xl bg-rose-50 px-4 py-3 text-xs font-semibold leading-5 text-rose-800 dark:bg-rose-950/40 dark:text-rose-200" role="alert">
                {recordMutationError instanceof Error ? recordMutationError.message : 'Taslak kaydedilemedi.'}
              </p>
            ) : null}
          </div>
        </div>

        <footer className="border-t border-app-border bg-app-surface px-4 py-3 sm:px-6 sm:py-4">
          {attentionTarget ? (
            <p className="mb-3 rounded-lg bg-amber-50 dark:bg-amber-950/40 px-3 py-2 text-xs font-semibold leading-5 text-amber-900 dark:text-amber-200" role="status" aria-live="polite">
              {attentionTarget === 'save'
                ? 'Yeni kayıt açmadan önce mevcut formu taslak olarak kaydedin veya kapatın.'
                : 'Taslağınız kaydedildi. Yeni bir form açmak için “Yeni Kayıt Oluştur”u kullanın.'}
            </p>
          ) : null}
          {draftSaved ? (
            <div>
              <div className="mb-3 flex items-center gap-2 text-xs font-bold text-emerald-700 dark:text-emerald-300" role="status">
                <CircleCheck className="size-4" aria-hidden="true" />
                Taslaklarım'a kaydedildi.
              </div>
              <div className="grid grid-cols-2 gap-3">
                <button
                  type="button"
                  onClick={onClose}
                  className="flex min-h-11 items-center justify-center rounded-xl border border-app-border px-3 text-xs font-bold text-app-text-secondary transition hover:bg-app-surface-muted hover:text-app-text focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500 sm:text-sm"
                >
                  Formu Kapat
                </button>
                <button
                  key={`new-record-${attentionVersion}`}
                  ref={newRecordButtonRef}
                  type="button"
                  onClick={startNewRecord}
                  className={`flex min-h-11 items-center justify-center gap-2 rounded-xl bg-brand-600 px-3 text-xs font-bold text-white shadow-lg shadow-brand-200 dark:shadow-black/20 transition hover:bg-brand-700 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500 sm:text-sm ${
                    attentionTarget === 'new' ? 'composer-action-nudge ring-2 ring-amber-300 dark:ring-amber-700 ring-offset-2' : ''
                  }`}
                >
                  <FilePlus2 className="size-4" aria-hidden="true" />
                  Yeni Kayıt Oluştur
                </button>
                <button
                  type="submit"
                  disabled={recordMutationBusy}
                  className="col-span-2 flex min-h-11 items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-brand-600 to-brand-500 px-4 text-xs font-bold text-white shadow-lg shadow-brand-200 dark:shadow-black/20 transition hover:from-brand-700 hover:to-brand-600 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500 disabled:cursor-not-allowed disabled:opacity-60 sm:px-5 sm:text-sm"
                >
                  İncelemeye Gönder
                  <Send className="size-4" aria-hidden="true" />
                </button>
              </div>
            </div>
          ) : (
            <>
              {feedback ? <p className="mb-2 text-xs font-semibold text-emerald-700 dark:text-emerald-300" role="status">{feedback}</p> : null}
              <div className="flex items-center justify-between gap-3">
                <button
                  key={`save-draft-${attentionVersion}`}
                  ref={saveButtonRef}
                  type="button"
                  onClick={saveDraft}
                  disabled={recordMutationBusy}
                  className={`flex min-h-11 items-center justify-center gap-2 rounded-xl border border-app-border px-3.5 text-xs font-bold text-app-text-secondary transition hover:border-brand-200 dark:hover:border-brand-700/60 hover:bg-brand-50 dark:hover:bg-brand-900/30 hover:text-brand-700 dark:hover:text-brand-300 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500 sm:text-sm ${
                    attentionTarget === 'save' ? 'composer-action-nudge border-amber-300 dark:border-amber-700 bg-amber-50 dark:bg-amber-950/40 ring-2 ring-amber-200 dark:ring-amber-800/70 ring-offset-2' : ''
                  }`}
                >
                  <Save className="size-4" aria-hidden="true" />
                  Taslak Kaydet
                </button>
                <button
                  type="submit"
                  disabled={recordMutationBusy}
                  className="flex min-h-11 items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-brand-600 to-brand-500 px-4 text-xs font-bold text-white shadow-lg shadow-brand-200 dark:shadow-black/20 transition hover:from-brand-700 hover:to-brand-600 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500 sm:px-5 sm:text-sm"
                >
                  İncelemeye Gönder
                  <Send className="size-4" aria-hidden="true" />
                </button>
              </div>
            </>
          )}
        </footer>
      </form>
      {discardDialogOpen ? (
        <div className="fixed inset-0 z-[110] flex items-end justify-center bg-slate-950/40 backdrop-blur-[2px] sm:items-center sm:p-4" role="presentation">
          <section
            ref={discardDialogRef}
            tabIndex={-1}
            role="dialog"
            aria-modal="true"
            aria-labelledby="discard-record-title"
            aria-describedby="discard-record-description"
            className="w-full rounded-t-3xl bg-app-surface p-5 shadow-2xl sm:max-w-md sm:rounded-2xl sm:p-6"
          >
            <h2 id="discard-record-title" className="text-lg font-bold text-app-text">Kaydedilmemiş değişiklikler var</h2>
            <p id="discard-record-description" className="mt-2 text-sm leading-6 text-app-text-muted">
              Formu kapatırsanız henüz taslak olarak kaydedilmemiş bilgiler silinecek.
            </p>
            <div className="mt-6 grid grid-cols-2 gap-3">
              <button
                ref={continueEditingRef}
                type="button"
                onClick={() => setDiscardDialogOpen(false)}
                className="min-h-11 rounded-xl border border-app-border px-4 text-sm font-bold text-app-text-secondary hover:bg-app-surface-muted focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
              >
                Düzenlemeye Devam Et
              </button>
              <button
                type="button"
                onClick={() => {
                  setDiscardDialogOpen(false)
                  onClose()
                }}
                className="min-h-11 rounded-xl bg-rose-600 px-4 text-sm font-bold text-white hover:bg-rose-700 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-rose-500"
              >
                Değişiklikleri Sil
              </button>
            </div>
          </section>
        </div>
      ) : null}
    </div>
  )
}

function FieldError({ id, message }: { id: string; message?: string }) {
  return (
    <p id={id} className="mt-1.5 text-xs font-semibold text-rose-700 dark:text-rose-300" role="alert">
      {message}
    </p>
  )
}
