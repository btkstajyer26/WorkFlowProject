import { uploadRecordFile, type RecordFile } from './files';

function jsonResponse(status: number, body: unknown): Response {
  return {
    headers: {
      get: () => 'application/json',
    },
    ok: status >= 200 && status < 300,
    status,
    text: jest.fn().mockResolvedValue(JSON.stringify(body)),
  } as unknown as Response;
}

describe('file API', () => {
  const fetchMock = jest.fn();
  const uploadedFile: RecordFile = {
    fileSize: 1024,
    id: 'file-id',
    mimeType: 'application/pdf',
    originalName: 'rapor.pdf',
    recordId: 'record-id',
    uploadedAt: '2026-08-27T10:00:00',
    uploadedBy: 'user-id',
  };

  beforeEach(() => {
    fetchMock.mockReset();
    globalThis.fetch = fetchMock as typeof fetch;
  });

  it('backendin dizi cevabından yüklenen dosyayı döndürür', async () => {
    fetchMock.mockResolvedValue(jsonResponse(200, [uploadedFile]));

    await expect(
      uploadRecordFile('record-id', {
        mimeType: 'application/pdf',
        name: 'rapor.pdf',
        uri: 'file:///rapor.pdf',
      }),
    ).resolves.toEqual(uploadedFile);
  });

  it('başarılı yanıtta dosya bilgisi yoksa hata verir', async () => {
    fetchMock.mockResolvedValue(jsonResponse(200, []));

    await expect(
      uploadRecordFile('record-id', {
        mimeType: 'application/pdf',
        name: 'rapor.pdf',
        uri: 'file:///rapor.pdf',
      }),
    ).rejects.toThrow('Sunucu yüklenen dosya bilgisini döndürmedi.');
  });
});
