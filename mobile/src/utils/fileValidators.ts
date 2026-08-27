export const MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB

const ALLOWED_EXTENSIONS_BY_MIME_TYPE: Record<string, readonly string[]> = {
  'application/pdf': ['.pdf'],
  'application/msword': ['.doc'],
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document': ['.docx'],
  'application/vnd.ms-excel': ['.xls'],
  'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet': ['.xlsx'],
  'image/png': ['.png'],
  'image/jpeg': ['.jpg', '.jpeg'],
};

export const ALLOWED_MIME_TYPES = Object.freeze(Object.keys(ALLOWED_EXTENSIONS_BY_MIME_TYPE));

export interface FileValidationResult {
  isValid: boolean;
  error?: string;
}

const getFileExtension = (fileName: string): string => {
  const lastDotIndex = fileName.lastIndexOf('.');

  if (lastDotIndex < 0) {
    return '';
  }

  return fileName.slice(lastDotIndex).toLocaleLowerCase('en-US');
};

export const validateFile = (file: { size?: number; mimeType?: string; name: string }): FileValidationResult => {
  if (!file.name.trim()) {
    return {
      isValid: false,
      error: 'Dosya adı belirlenemedi.',
    };
  }

  if (file.size === 0) {
    return {
      isValid: false,
      error: 'Boş dosya yüklenemez.',
    };
  }

  if (file.size !== undefined && file.size > MAX_FILE_SIZE_BYTES) {
    return {
      isValid: false,
      error: `Dosya boyutu 10 MB sınırını aşıyor (${(file.size / (1024 * 1024)).toFixed(2)} MB).`,
    };
  }

  const mimeType = file.mimeType?.trim().toLocaleLowerCase('en-US');

  if (!mimeType) {
    return {
      isValid: false,
      error: 'Dosya türü belirlenemedi. Lütfen PDF, Word, Excel, PNG veya JPEG dosyası seçin.',
    };
  }

  const allowedExtensions = ALLOWED_EXTENSIONS_BY_MIME_TYPE[mimeType];

  if (!allowedExtensions) {
    return {
      isValid: false,
      error: 'Desteklenmeyen dosya formatı. PDF, Word, Excel, PNG veya JPEG dosyası seçin.',
    };
  }

  const extension = getFileExtension(file.name);

  if (!allowedExtensions.includes(extension)) {
    return {
      isValid: false,
      error: 'Dosya uzantısı ile dosya türü uyuşmuyor.',
    };
  }

  return { isValid: true };
};
