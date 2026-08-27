import {
  ALLOWED_MIME_TYPES,
  MAX_FILE_SIZE_BYTES,
  validateFile,
} from './fileValidators';

describe('validateFile', () => {
  it.each([
    ['rapor.pdf', 'application/pdf'],
    ['belge.doc', 'application/msword'],
    ['belge.DOCX', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'],
    ['tablo.xls', 'application/vnd.ms-excel'],
    ['tablo.xlsx', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'],
    ['gorsel.png', 'image/png'],
    ['fotograf.jpg', 'image/jpeg'],
    ['fotograf.JPEG', 'image/jpeg'],
  ])('%s dosyasını kabul eder', (name, mimeType) => {
    expect(validateFile({ name, mimeType, size: 1024 })).toEqual({ isValid: true });
  });

  it('backendin desteklemediği WebP ve metin dosyalarını kabul etmez', () => {
    expect(validateFile({ name: 'gorsel.webp', mimeType: 'image/webp' }).isValid).toBe(false);
    expect(validateFile({ name: 'not.txt', mimeType: 'text/plain' }).isValid).toBe(false);
  });

  it('MIME türü belirlenemeyen dosyayı kabul etmez', () => {
    expect(validateFile({ name: 'rapor.pdf' })).toEqual({
      isValid: false,
      error: 'Dosya türü belirlenemedi. Lütfen PDF, Word, Excel, PNG veya JPEG dosyası seçin.',
    });
  });

  it('MIME türü ile dosya uzantısı uyuşmayan dosyayı kabul etmez', () => {
    expect(validateFile({ name: 'zararli.pdf', mimeType: 'application/x-msdownload' }).isValid).toBe(false);
    expect(validateFile({ name: 'rapor.exe', mimeType: 'application/pdf' })).toEqual({
      isValid: false,
      error: 'Dosya uzantısı ile dosya türü uyuşmuyor.',
    });
  });

  it('boş dosyayı kabul etmez', () => {
    expect(validateFile({ name: 'bos.pdf', mimeType: 'application/pdf', size: 0 })).toEqual({
      isValid: false,
      error: 'Boş dosya yüklenemez.',
    });
  });

  it('10 MB sınırını aşan dosyayı kabul etmez', () => {
    expect(
      validateFile({
        name: 'buyuk.pdf',
        mimeType: 'application/pdf',
        size: MAX_FILE_SIZE_BYTES + 1,
      }).isValid,
    ).toBe(false);
  });

  it('picker filtresini backendin kabul ettiği yedi MIME türüyle sınırlar', () => {
    expect(ALLOWED_MIME_TYPES).toEqual([
      'application/pdf',
      'application/msword',
      'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
      'application/vnd.ms-excel',
      'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      'image/png',
      'image/jpeg',
    ]);
  });
});
