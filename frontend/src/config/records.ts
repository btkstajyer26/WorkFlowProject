export const maxRecordTitleLength = 255

export const allowedAttachmentExtensions = [
  '.pdf',
  '.doc',
  '.docx',
  '.xls',
  '.xlsx',
  '.png',
  '.jpg',
  '.jpeg',
] as const

export const attachmentAcceptValue = allowedAttachmentExtensions.join(',')

const allowedAttachmentExtensionSet = new Set<string>(allowedAttachmentExtensions)

export function getAttachmentValidationError(files: File[]) {
  const unsupportedFile = files.find((file) => {
    const normalizedName = file.name.trim().toLowerCase()
    const extensionStart = normalizedName.lastIndexOf('.')
    const extension = extensionStart >= 0 ? normalizedName.slice(extensionStart) : ''
    return !allowedAttachmentExtensionSet.has(extension)
  })

  return unsupportedFile
    ? `“${unsupportedFile.name}” desteklenmiyor. PDF, Word, Excel, PNG veya JPG yükleyin.`
    : null
}
