import { fireEvent, render, screen } from '@testing-library/react-native';

import type { RecordFile } from '@/api/files';
import { FileList } from './FileList';

jest.mock('@/api/files', () => ({
  downloadAndOpenFile: jest.fn().mockResolvedValue({ shared: true, uri: 'file:///path/test.pdf' }),
}));

const mockFiles: RecordFile[] = [
  {
    fileSize: 2048,
    id: 'f1-1111-1111-1111-111111111111',
    mimeType: 'application/pdf',
    originalName: 'sozlesme.pdf',
    recordId: 'r1-1111-1111-1111-111111111111',
    uploadedAt: '2026-08-27T10:00:00Z',
    uploadedBy: 'u1-1111-1111-1111-111111111111',
  },
];

describe('FileList', () => {
  it('dosya listesi boşken EmptyState render eder', async () => {
    await render(<FileList files={[]} />);
    expect(screen.getByText('Dosya Yok')).toBeTruthy();
    expect(
      screen.getByText('Bu kayda henüz bir dosya eklenmemiş.'),
    ).toBeTruthy();
  });

  it('dosyaları ve "Aç / Paylaş" butonunu doğru şekilde render eder', async () => {
    await render(<FileList files={mockFiles} />);
    expect(screen.getByText('sozlesme.pdf')).toBeTruthy();
    expect(screen.getByText('Aç / Paylaş')).toBeTruthy();
  });

  it('canDelete true ve onDeleteFile verildiğinde silme butonunu gösterir ve tıklandığında çağırır', async () => {
    const onDeleteFileMock = jest.fn();
    await render(
      <FileList
        canDelete={true}
        files={mockFiles}
        onDeleteFile={onDeleteFileMock}
      />,
    );

    const deleteButton = screen.getByLabelText('sozlesme.pdf dosyasını sil');
    expect(deleteButton).toBeTruthy();

    fireEvent.press(deleteButton);
    expect(onDeleteFileMock).toHaveBeenCalledWith('f1-1111-1111-1111-111111111111');
  });
});
