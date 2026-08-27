import { fireEvent, render, screen } from '@testing-library/react-native';

import type { RecordDetail } from '@/api/records';
import type { CurrentUser } from '@/api/users';
import { RecordWorkflowActions } from './RecordWorkflowActions';
import { useRecordWorkflow } from '@/query/workflow';
import { createWrapper } from '@/test-utils/testWrapper';

jest.mock('@/query/workflow', () => ({
  useRecordWorkflow: jest.fn().mockReturnValue({
    isPending: false,
    mutateAsync: jest.fn(),
  }),
}));

const mockRecord: RecordDetail = {
  categoryId: 1,
  createdAt: '2026-08-27T10:00:00Z',
  createdBy: 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
  createdByFullName: 'Ahmet Yılmaz',
  description: 'Test evrak açıklaması',
  id: 'd3b07384-d113-4632-8fe2-51a6597a7a58',
  status: 'TASLAK',
  title: 'Test Evrak Başlığı',
};

const mockCalisanUser: CurrentUser = {
  active: true,
  createdAt: '2026-08-27T10:00:00Z',
  email: 'calisan@test.local',
  firstName: 'Ahmet',
  id: 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
  lastName: 'Yılmaz',
  roleName: 'CALISAN',
};

const mockBaskanYrdUser: CurrentUser = {
  active: true,
  createdAt: '2026-08-27T10:00:00Z',
  email: 'bskyrd@test.local',
  firstName: 'Mehmet',
  id: 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a22',
  lastName: 'Kaya',
  roleName: 'BASKAN_YARDIMCISI',
};

const mockBaskanUser: CurrentUser = {
  active: true,
  createdAt: '2026-08-27T10:00:00Z',
  email: 'baskan@test.local',
  firstName: 'Ayşe',
  id: 'c2eebc99-9c0b-4ef8-bb6d-6bb9bd380a33',
  lastName: 'Demir',
  roleName: 'BASKAN',
};

describe('RecordWorkflowActions', () => {
  it('çalışan kendi taslak evrağında "İncelemeye gönder" butonunu görür', async () => {
    await render(
      <RecordWorkflowActions
        record={mockRecord}
        user={mockCalisanUser}
      />,
      { wrapper: createWrapper() },
    );

    expect(screen.getByText('İncelemeye gönder')).toBeTruthy();
  });

  it('çalışan kendi "DUZENLEME_BEKLIYOR" evrağında "Tekrar gönder" butonunu görür', async () => {
    await render(
      <RecordWorkflowActions
        record={{ ...mockRecord, status: 'DUZENLEME_BEKLIYOR' }}
        user={mockCalisanUser}
      />,
      { wrapper: createWrapper() },
    );

    expect(screen.getByText('Tekrar gönder')).toBeTruthy();
  });

  it('evrağın sahibi olmayan çalışan aksiyon butonları görmez', async () => {
    await render(
      <RecordWorkflowActions
        record={mockRecord}
        user={{ ...mockCalisanUser, id: 'f9eebc99-9c0b-4ef8-bb6d-6bb9bd380a99' }}
      />,
      { wrapper: createWrapper() },
    );

    expect(screen.queryByText('İncelemeye gönder')).toBeNull();
  });

  it('başkan yardımcısı "BSK_YRD_INCELEMESINDE" durumunda ilgili butonları görür', async () => {
    await render(
      <RecordWorkflowActions
        record={{ ...mockRecord, status: 'BSK_YRD_INCELEMESINDE' }}
        user={mockBaskanYrdUser}
      />,
      { wrapper: createWrapper() },
    );

    expect(screen.getByText('Başkana ilet')).toBeTruthy();
    expect(screen.getByText('Çalışana geri gönder')).toBeTruthy();
  });

  it('başkan "BASKAN_INCELEMESINDE" durumunda 4 aksiyon butonunu görür', async () => {
    await render(
      <RecordWorkflowActions
        record={{ ...mockRecord, status: 'BASKAN_INCELEMESINDE' }}
        user={mockBaskanUser}
      />,
      { wrapper: createWrapper() },
    );

    expect(screen.getByText('Onayla')).toBeTruthy();
    expect(screen.getByText('Reddet')).toBeTruthy();
    expect(screen.getByText('Çalışana geri gönder')).toBeTruthy();
    expect(screen.getByText('Başkan yardımcısına geri gönder')).toBeTruthy();
  });

  it('onaylama butonuna tıklandığında modal açılır ve işlemi onayla dendiğinde mutation çalışır', async () => {
    const mutateAsyncMock = jest.fn().mockResolvedValue({});
    (useRecordWorkflow as jest.Mock).mockReturnValue({
      isPending: false,
      mutateAsync: mutateAsyncMock,
    });

    await render(
      <RecordWorkflowActions
        record={{ ...mockRecord, status: 'BASKAN_INCELEMESINDE' }}
        user={mockBaskanUser}
      />,
      { wrapper: createWrapper() },
    );

    const onaylaButton = screen.getByText('Onayla');
    fireEvent.press(onaylaButton);

    const confirmButton = await screen.findByText('İşlemi onayla');
    fireEvent.press(confirmButton);

    expect(mutateAsyncMock).toHaveBeenCalledWith({
      action: 'ONAYLA',
    });
  });

  it('açıklama zorunlu olan aksiyonda (örn: Reddet) boş açıklama gönderilirse hata verir', async () => {
    const mutateAsyncMock = jest.fn().mockResolvedValue({});
    (useRecordWorkflow as jest.Mock).mockReturnValue({
      isPending: false,
      mutateAsync: mutateAsyncMock,
    });

    await render(
      <RecordWorkflowActions
        record={{ ...mockRecord, status: 'BASKAN_INCELEMESINDE' }}
        user={mockBaskanUser}
      />,
      { wrapper: createWrapper() },
    );

    const reddetButton = screen.getByText('Reddet');
    fireEvent.press(reddetButton);

    const confirmButton = await screen.findByText('İşlemi onayla');
    fireEvent.press(confirmButton);

    expect(await screen.findByText('Bu işlem için açıklama zorunludur.')).toBeTruthy();
    expect(mutateAsyncMock).not.toHaveBeenCalled();
  });
});
