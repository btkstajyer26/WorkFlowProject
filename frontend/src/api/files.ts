import type { FileResponseDto } from './generated/data-contracts'
import { api } from './client'
import { ApiClientError } from './errors'

export type RecordFile = {
  id: string
  recordId: string
  originalName: string
  mimeType: string
  fileSize: number
  uploadedBy: string
  uploadedAt: string
}

function invalidFileResponse(message: string): never {
  throw new ApiClientError({
    code: 'INVALID_FILE_RESPONSE',
    message,
    status: 0,
  })
}

function normalizeRecordFile(file: FileResponseDto): RecordFile {
  if (
    !file.id ||
    !file.recordId ||
    !file.originalName?.trim() ||
    !file.mimeType?.trim() ||
    typeof file.fileSize !== 'number' ||
    file.fileSize < 0 ||
    !file.uploadedBy ||
    !file.uploadedAt
  ) {
    return invalidFileResponse('Sunucu geçerli dosya bilgisi döndürmedi.')
  }

  return {
    id: file.id,
    recordId: file.recordId,
    originalName: file.originalName.trim(),
    mimeType: file.mimeType.trim(),
    fileSize: file.fileSize,
    uploadedBy: file.uploadedBy,
    uploadedAt: file.uploadedAt,
  }
}

export async function listRecordFiles(recordId: string) {
  const files = await api.files.listFiles({ id: recordId })
  return files.map(normalizeRecordFile).toSorted((left, right) => left.uploadedAt.localeCompare(right.uploadedAt))
}

export async function uploadRecordFile(recordId: string, file: File) {
  const response = await api.files.uploadFiles({ id: recordId }, { file: [file] })
  const uploadedFile = response[0]
  if (!uploadedFile) return invalidFileResponse('Sunucu yüklenen dosya bilgisini döndürmedi.')
  return normalizeRecordFile(uploadedFile)
}

export function deleteRecordFile(fileId: string) {
  return api.files.deleteFile({ id: fileId })
}

export async function getRecordFileBlob(fileId: string, preview = false) {
  const response = preview
    ? await api.files.previewFile({ id: fileId }, { format: 'blob' })
    : await api.files.downloadFile({ id: fileId }, { format: 'blob' })
  return response as unknown as Blob
}

export function formatFileSize(size: number) {
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${Math.max(1, Math.round(size / 1024))} KB`
  return `${(size / (1024 * 1024)).toLocaleString('tr-TR', { maximumFractionDigits: 1 })} MB`
}
