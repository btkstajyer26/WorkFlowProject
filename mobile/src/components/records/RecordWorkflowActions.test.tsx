import { fireEvent, render, screen } from '@testing-library/react-native';

import type { RecordDetail } from '@/api/records';
import { RecordWorkflowActions } from './RecordWorkflowActions';
import {
  useAvailableWorkflowActions,
  useRecordWorkflow,
} from '@/query/workflow';
import { createWrapper } from '@/test-utils/testWrapper';

jest.mock('@/query/workflow', () => ({
  useAvailableWorkflowActions: jest.fn(),
  useRecordWorkflow: jest.fn(),
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

const baseAvailableActionsResponse = {
  recordId: mockRecord.id,
  status: mockRecord.status,
  version: 1,
};

describe('RecordWorkflowActions', () => {
  beforeEach(() => {
    jest.clearAllMocks();

    (useRecordWorkflow as jest.Mock).mockReturnValue({
      isPending: false,
      mutateAsync: jest.fn(),
    });

    (useAvailableWorkflowActions as jest.Mock).mockReturnValue({
      data: {
        ...baseAvailableActionsResponse,
        actions: [],
      },
      isError: false,
      isPending: false,
    });
  });

  it('backend tarafından dönen aksiyonun displayName değerini gösterir', async () => {
    (useAvailableWorkflowActions as jest.Mock).mockReturnValue({
      data: {
        ...baseAvailableActionsResponse,
        actions: [
          {
            action: 'GONDER',
            commentRequired: false,
            displayName: 'İncelemeye gönder',
            targetDepartmentRequired: false,
            targetUserRequired: false,
          },
        ],
      },
      isError: false,
      isPending: false,
    });

    await render(<RecordWorkflowActions record={mockRecord} />, {
      wrapper: createWrapper(),
    });

    expect(screen.getByText('İncelemeye gönder')).toBeTruthy();
  });

  it('backend boş aksiyon listesi döndürürse işlem alanını göstermez', async () => {
    await render(<RecordWorkflowActions record={mockRecord} />, {
      wrapper: createWrapper(),
    });

    expect(screen.queryByText('Kayıt işlemleri')).toBeNull();
  });

  it('targetDepartmentRequired aksiyonunu MOB-1 tamamlanana kadar göstermez', async () => {
    (useAvailableWorkflowActions as jest.Mock).mockReturnValue({
      data: {
        ...baseAvailableActionsResponse,
        actions: [
          {
            action: 'DEPARTMANA_GONDER',
            commentRequired: false,
            displayName: 'Departmana gönder',
            targetDepartmentRequired: true,
            targetUserRequired: false,
          },
        ],
      },
      isError: false,
      isPending: false,
    });

    await render(<RecordWorkflowActions record={mockRecord} />, {
      wrapper: createWrapper(),
    });

    expect(screen.queryByText('Departmana gönder')).toBeNull();
  });

  it('targetUserRequired aksiyonunu hedef kullanıcı seçimi desteklenene kadar göstermez', async () => {
    (useAvailableWorkflowActions as jest.Mock).mockReturnValue({
      data: {
        ...baseAvailableActionsResponse,
        actions: [
          {
            action: 'GONDER',
            commentRequired: false,
            displayName: 'Hedef kullanıcıya gönder',
            targetDepartmentRequired: false,
            targetUserRequired: true,
          },
        ],
      },
      isError: false,
      isPending: false,
    });

    await render(<RecordWorkflowActions record={mockRecord} />, {
      wrapper: createWrapper(),
    });

    expect(screen.queryByText('Hedef kullanıcıya gönder')).toBeNull();
  });

  it('seçilen backend aksiyonunu mutation ile gönderir', async () => {
    const mutateAsyncMock = jest.fn().mockResolvedValue({});

    (useRecordWorkflow as jest.Mock).mockReturnValue({
      isPending: false,
      mutateAsync: mutateAsyncMock,
    });

    (useAvailableWorkflowActions as jest.Mock).mockReturnValue({
      data: {
        ...baseAvailableActionsResponse,
        actions: [
          {
            action: 'ONAYLA',
            commentRequired: false,
            displayName: 'Onayla',
            targetDepartmentRequired: false,
            targetUserRequired: false,
          },
        ],
      },
      isError: false,
      isPending: false,
    });

    await render(<RecordWorkflowActions record={mockRecord} />, {
      wrapper: createWrapper(),
    });

    fireEvent.press(screen.getByText('Onayla'));
    fireEvent.press(await screen.findByText('İşlemi onayla'));

    expect(mutateAsyncMock).toHaveBeenCalledWith({
      action: 'ONAYLA',
    });
  });

  it('commentRequired aksiyonunda boş açıklamayı göndermez', async () => {
    const mutateAsyncMock = jest.fn().mockResolvedValue({});

    (useRecordWorkflow as jest.Mock).mockReturnValue({
      isPending: false,
      mutateAsync: mutateAsyncMock,
    });

    (useAvailableWorkflowActions as jest.Mock).mockReturnValue({
      data: {
        ...baseAvailableActionsResponse,
        actions: [
          {
            action: 'REDDET',
            commentRequired: true,
            displayName: 'Reddet',
            targetDepartmentRequired: false,
            targetUserRequired: false,
          },
        ],
      },
      isError: false,
      isPending: false,
    });

    await render(<RecordWorkflowActions record={mockRecord} />, {
      wrapper: createWrapper(),
    });

    fireEvent.press(screen.getByText('Reddet'));
    fireEvent.press(await screen.findByText('İşlemi onayla'));

    expect(
      await screen.findByText('Bu işlem için açıklama zorunludur.'),
    ).toBeTruthy();
    expect(mutateAsyncMock).not.toHaveBeenCalled();
  });
});
