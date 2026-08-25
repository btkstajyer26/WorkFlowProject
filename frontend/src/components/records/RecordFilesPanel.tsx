import { Download, Eye, FileText, LoaderCircle, Paperclip, Trash2 } from 'lucide-react'
import { useState } from 'react'
import { formatFileSize, getRecordFileBlob, type RecordFile } from '../../api/files'
import { attachmentAcceptValue, getAttachmentValidationError } from '../../config/records'
import { useRecordFiles } from '../../hooks/useRecordFiles'

const uploadedAtFormatter = new Intl.DateTimeFormat('tr-TR', {
  dateStyle: 'medium',
  timeStyle: 'short',
})

export function RecordFilesPanel({ recordId, editable = false }: { recordId: string; editable?: boolean }) {
  const files = useRecordFiles(recordId)
  const [actionError, setActionError] = useState<string | null>(null)
  const [busyFileId, setBusyFileId] = useState<string | null>(null)

  const runBlobAction = async (file: RecordFile, preview: boolean) => {
    setActionError(null)
    setBusyFileId(file.id)
    const previewWindow = preview ? window.open('about:blank', '_blank') : null
    if (previewWindow) previewWindow.opener = null

    try {
      const blob = await getRecordFileBlob(file.id, preview)
      const objectUrl = URL.createObjectURL(blob)
      if (preview) {
        if (previewWindow) previewWindow.location.replace(objectUrl)
        else window.open(objectUrl, '_blank', 'noopener,noreferrer')
      } else {
        const link = document.createElement('a')
        link.href = objectUrl
        link.download = file.originalName
        link.click()
      }
      window.setTimeout(() => URL.revokeObjectURL(objectUrl), 60_000)
    } catch (error) {
      previewWindow?.close()
      setActionError(error instanceof Error ? error.message : 'Dosya işlemi tamamlanamadı.')
    } finally {
      setBusyFileId(null)
    }
  }

  const uploadSelectedFiles = async (selectedFiles: File[]) => {
    const validationError = getAttachmentValidationError(selectedFiles)
    if (validationError) {
      setActionError(validationError)
      return
    }

    setActionError(null)
    try {
      for (const file of selectedFiles) await files.upload(file)
    } catch (error) {
      setActionError(error instanceof Error ? error.message : 'Dosya yüklenemedi.')
    }
  }

  const removeFile = async (file: RecordFile) => {
    setActionError(null)
    try {
      await files.remove(file.id)
    } catch (error) {
      setActionError(error instanceof Error ? error.message : 'Dosya silinemedi.')
    }
  }

  const visibleError = actionError ?? (files.error instanceof Error ? files.error.message : null)

  return (
    <div>
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-base font-bold text-app-text">Ek Dosyalar</h2>
          <p className="mt-0.5 text-xs text-app-text-subtle">PDF, Word, Excel, JPG veya PNG · en fazla 10 MB</p>
        </div>
        {editable ? (
          <label className="flex min-h-10 cursor-pointer items-center gap-2 rounded-xl border border-app-border px-3 text-xs font-bold text-app-text-secondary transition hover:border-brand-300 hover:text-brand-700 focus-within:outline-2 focus-within:outline-brand-500 dark:hover:border-brand-600 dark:hover:text-brand-300">
            {files.uploading ? <LoaderCircle className="size-4 animate-spin" aria-hidden="true" /> : <Paperclip className="size-4" aria-hidden="true" />}
            {files.uploading ? 'Yükleniyor…' : 'Dosya ekle'}
            <input
              type="file"
              multiple
              disabled={files.uploading}
              accept={attachmentAcceptValue}
              className="sr-only"
              onChange={(event) => {
                void uploadSelectedFiles(Array.from(event.target.files ?? []))
                event.target.value = ''
              }}
            />
          </label>
        ) : null}
      </div>

      {visibleError ? (
        <div className="mt-3 rounded-xl border border-rose-200 bg-rose-50 px-3 py-2.5 text-xs font-semibold text-rose-800 dark:border-rose-900/70 dark:bg-rose-950/40 dark:text-rose-200" role="alert">
          <p>{visibleError}</p>
          {files.error ? <button type="button" onClick={() => void files.retry()} className="mt-2 underline">Tekrar dene</button> : null}
        </div>
      ) : null}

      {files.isPending ? (
        <p className="mt-4 rounded-xl border border-dashed border-app-border px-4 py-5 text-center text-sm text-app-text-subtle" role="status">Dosyalar yükleniyor…</p>
      ) : files.error ? null : files.files.length > 0 ? (
        <ul className="mt-4 space-y-2" aria-label="Kayıt ekleri">
          {files.files.map((file) => {
            const busy = busyFileId === file.id || files.deletingId === file.id
            return (
              <li key={file.id} className="flex items-center gap-3 rounded-xl border border-app-border px-3 py-2.5 sm:px-4">
                <FileText className="size-4 shrink-0 text-app-text-faint" aria-hidden="true" />
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-bold text-app-text-emphasis">{file.originalName}</p>
                  <p className="mt-0.5 text-xs text-app-text-subtle">{formatFileSize(file.fileSize)} · {uploadedAtFormatter.format(new Date(file.uploadedAt))}</p>
                </div>
                <div className="flex shrink-0 items-center gap-1">
                  <FileActionButton label={`${file.originalName} dosyasını önizle`} disabled={busy} onClick={() => void runBlobAction(file, true)} icon={Eye} />
                  <FileActionButton label={`${file.originalName} dosyasını indir`} disabled={busy} onClick={() => void runBlobAction(file, false)} icon={Download} />
                  {editable ? <FileActionButton label={`${file.originalName} dosyasını sil`} disabled={busy} onClick={() => void removeFile(file)} icon={Trash2} danger /> : null}
                </div>
              </li>
            )
          })}
        </ul>
      ) : (
        <p className="mt-4 rounded-xl border border-dashed border-app-border px-4 py-5 text-center text-sm text-app-text-subtle">Bu kayda eklenmiş dosya yok.</p>
      )}
    </div>
  )
}

function FileActionButton({
  label,
  disabled,
  onClick,
  icon: Icon,
  danger = false,
}: {
  label: string
  disabled: boolean
  onClick: () => void
  icon: typeof Download
  danger?: boolean
}) {
  return (
    <button
      type="button"
      aria-label={label}
      disabled={disabled}
      onClick={onClick}
      className={`flex size-9 items-center justify-center rounded-lg transition focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500 disabled:opacity-50 ${danger ? 'text-rose-600 hover:bg-rose-50 dark:text-rose-400 dark:hover:bg-rose-950/40' : 'text-app-text-subtle hover:bg-brand-50 hover:text-brand-700 dark:hover:bg-brand-900/30 dark:hover:text-brand-300'}`}
    >
      <Icon className="size-4" aria-hidden="true" />
    </button>
  )
}
