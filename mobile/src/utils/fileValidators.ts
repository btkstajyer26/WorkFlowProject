export const MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB

export const ALLOWED_MIME_TYPES = [
  'image/jpeg',
  'image/png',
  'image/webp',
  'application/pdf',
  'text/plain',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document', // docx
  'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', // xlsx
];

export interface FileValidationResult {
  isValid: boolean;
  error?: string;
}

export const validateFile = (file: { size?: number; mimeType?: string; name: string }): FileValidationResult => {
  if (file.size && file.size > MAX_FILE_SIZE_BYTES) {
    return {
      isValid: false,
      error: `Dosya boyutu 10 MB sınırını aşıyor (${(file.size / (1024 * 1024)).toFixed(2)} MB).`,
    };
  }

  if (file.mimeType && !ALLOWED_MIME_TYPES.includes(file.mimeType)) {
    return {
      isValid: false,
      error: 'Desteklenmeyen dosya formatı. Lütfen geçerli bir belge veya görsel seçin.',
    };
  }

  return { isValid: true };
};