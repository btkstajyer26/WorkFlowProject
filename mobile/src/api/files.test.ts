import * as FileSystem from 'expo-file-system/legacy';
import * as IntentLauncher from 'expo-intent-launcher';
import * as Sharing from 'expo-sharing';
import { Platform } from 'react-native';

import {
  downloadAndOpenFile,
  openOrShareFile,
  uploadRecordFile,
  type RecordFile,
} from './files';

jest.mock('expo-sharing', () => ({
  isAvailableAsync: jest.fn(),
  shareAsync: jest.fn(),
}));

jest.mock('expo-intent-launcher', () => ({
  startActivityAsync: jest.fn(),
}));

jest.mock('expo-file-system/legacy', () => ({
  cacheDirectory: 'file:///cache/',
  documentDirectory: 'file:///documents/',
  downloadAsync: jest.fn(),
  getContentUriAsync: jest.fn(),
}));

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
  const originalPlatformOS = Platform.OS;
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
    jest.clearAllMocks();
    globalThis.fetch = fetchMock as typeof fetch;
    Object.defineProperty(Platform, 'OS', {
      configurable: true,
      value: 'ios',
    });
  });

  afterAll(() => {
    Object.defineProperty(Platform, 'OS', {
      configurable: true,
      value: originalPlatformOS,
    });
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

  describe('openOrShareFile', () => {
    it('Android cihazda dosyayı önce uygun görüntüleyiciyle açar', async () => {
      Object.defineProperty(Platform, 'OS', {
        configurable: true,
        value: 'android',
      });
      (FileSystem.getContentUriAsync as jest.Mock).mockResolvedValue(
        'content://documents/rapor.pdf',
      );
      (IntentLauncher.startActivityAsync as jest.Mock).mockResolvedValue({
        resultCode: -1,
      });

      const result = await openOrShareFile('file:///documents/rapor.pdf', {
        mimeType: 'application/pdf',
      });

      expect(result).toBe(true);
      expect(IntentLauncher.startActivityAsync).toHaveBeenCalledWith(
        'android.intent.action.VIEW',
        {
          data: 'content://documents/rapor.pdf',
          flags: 1,
          type: 'application/pdf',
        },
      );
      expect(Sharing.shareAsync).not.toHaveBeenCalled();
    });

    it('Android görüntüleyicisi açılamazsa paylaşım menüsüne düşer', async () => {
      Object.defineProperty(Platform, 'OS', {
        configurable: true,
        value: 'android',
      });
      (FileSystem.getContentUriAsync as jest.Mock).mockResolvedValue(
        'content://documents/rapor.pdf',
      );
      (IntentLauncher.startActivityAsync as jest.Mock).mockRejectedValue(
        new Error('Uygun görüntüleyici yok'),
      );
      (Sharing.isAvailableAsync as jest.Mock).mockResolvedValue(true);

      const result = await openOrShareFile('file:///documents/rapor.pdf', {
        mimeType: 'application/pdf',
      });

      expect(result).toBe(true);
      expect(Sharing.shareAsync).toHaveBeenCalled();
    });

    it('paylaşım desteklenmiyorsa false döndürür', async () => {
      (Sharing.isAvailableAsync as jest.Mock).mockResolvedValue(false);

      const result = await openOrShareFile('file:///documents/rapor.pdf');
      expect(result).toBe(false);
      expect(Sharing.shareAsync).not.toHaveBeenCalled();
    });

    it('paylaşım destekleniyorsa shareAsync çağırır ve true döndürür', async () => {
      (Sharing.isAvailableAsync as jest.Mock).mockResolvedValue(true);
      (Sharing.shareAsync as jest.Mock).mockResolvedValue(undefined);

      const result = await openOrShareFile('file:///documents/rapor.pdf', {
        dialogTitle: 'rapor.pdf',
        mimeType: 'application/pdf',
      });
      expect(result).toBe(true);
      expect(Sharing.shareAsync).toHaveBeenCalledWith(
        'file:///documents/rapor.pdf',
        {
          UTI: undefined,
          dialogTitle: 'rapor.pdf',
          mimeType: 'application/pdf',
        },
      );
    });
  });

  describe('downloadAndOpenFile', () => {
    it('dosyayı indirip paylaşım diyalogunu tetikler', async () => {
      (FileSystem.downloadAsync as jest.Mock).mockResolvedValue({
        status: 200,
        uri: 'file:///documents/rapor.pdf',
      });
      (Sharing.isAvailableAsync as jest.Mock).mockResolvedValue(true);
      (Sharing.shareAsync as jest.Mock).mockResolvedValue(undefined);

      const result = await downloadAndOpenFile(
        'file-id-123',
        'rapor.pdf',
        'application/pdf',
      );

      expect(result).toEqual({
        shared: true,
        uri: 'file:///documents/rapor.pdf',
      });
      expect(FileSystem.downloadAsync).toHaveBeenCalled();
      expect(Sharing.shareAsync).toHaveBeenCalled();
    });
  });
});

