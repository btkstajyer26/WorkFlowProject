import { fireEvent, render, screen } from '@testing-library/react-native';

import type { RecordDetail } from '@/api/records';
import { RecordWorkflowActions } from './RecordWorkflowActions';
import {
  useAvailableWorkflowActions,
  useRecordWorkflow,
  useWorkflowTargetDepartments,
} from '@/query/workflow';
import { createWrapper } from '@/test-utils/testWrapper';

jest.mock('@/query/workflow', () => ({
  useAvailableWorkflowActions: jest.fn(),
  useRecordWorkflow: jest.fn(),
  useWorkflowTargetDepartments: jest.fn(),
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
    (useWorkflowTargetDepartments as jest.Mock).mockReturnValue({
      data: { departments: [{ id: 12, name: 'Hukuk' }, { id: 15, name: 'Satın Alma' }] },
      isPending: false, isFetching: false, isError: false, refetch: jest.fn(),
    });

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

  it('targetDepartmentRequired aksiyonunu gösterir', async () => {
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

    expect(screen.getByText('Departmana gönder')).toBeTruthy();
  });

  it('sözleşmede desteklenmeyen kullanıcı hedefini gizlemez ama göndermez', async () => {
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

    await fireEvent.press(screen.getByText('Hedef kullanıcıya gönder'));
    expect(screen.getByText('Bu işlem için kullanıcı seçimi şu anda desteklenmiyor.')).toBeTruthy();
    await fireEvent.press(screen.getByText('İşlemi onayla'));
    expect((useRecordWorkflow as jest.Mock).mock.results[0].value.mutateAsync).not.toHaveBeenCalled();
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

    await fireEvent.press(screen.getByText('Onayla'));
    await fireEvent.press(await screen.findByText('İşlemi onayla'));

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

    await fireEvent.press(screen.getByText('Reddet'));
    await fireEvent.press(await screen.findByText('İşlemi onayla'));

    expect(
      await screen.findByText('Bu işlem için açıklama zorunludur.'),
    ).toBeTruthy();
    expect(mutateAsyncMock).not.toHaveBeenCalled();
  });
});

const departmentAction = {
  action: 'DEPARTMANA_GONDER', displayName: 'Hukuk birimine yönlendir',
  commentRequired: false, targetDepartmentRequired: true, targetUserRequired: false,
};

// These flags and labels come from the server, independently of the user's role.
describe('workflow hedef departman seçimi', () => {
  const mutateAsync = jest.fn();
  const refetch = jest.fn();
  const options = [{ id: 12, name: 'Hukuk' }, { id: 15, name: 'Satın Alma' }];
  function targetState(overrides = {}) {
    (useWorkflowTargetDepartments as jest.Mock).mockReturnValue({
      data: { departments: options }, isPending: false, isFetching: false,
      isError: false, refetch, ...overrides,
    });
  }
  async function open() {
    await render(<RecordWorkflowActions record={mockRecord} />, { wrapper: createWrapper() });
    await fireEvent.press(screen.getByText(departmentAction.displayName));
  }
  beforeEach(() => {
    jest.clearAllMocks();
    mutateAsync.mockResolvedValue({});
    (useRecordWorkflow as jest.Mock).mockReturnValue({ isPending: false, mutateAsync });
    (useAvailableWorkflowActions as jest.Mock).mockReturnValue({
      data: { ...baseAvailableActionsResponse, actions: [departmentAction,
        { ...departmentAction, action: 'ONAYLA', displayName: 'Son onay', targetDepartmentRequired: false }] },
      isPending: false, isError: false,
    });
    targetState();
  });
  it('yalnız seçim gereken modal açıldığında seçenekleri ister', async () => {
    await open();
    expect(useWorkflowTargetDepartments).toHaveBeenNthCalledWith(1, mockRecord.id, false);
    expect(useWorkflowTargetDepartments).toHaveBeenLastCalledWith(mockRecord.id, true);
  });
  it('departman seçmeden göndermez', async () => {
    await open();
    await fireEvent.press(screen.getByText('İşlemi onayla'));
    expect(screen.getByText('Bir hedef departman seçin.')).toBeTruthy();
    expect(mutateAsync).not.toHaveBeenCalled();
  });
  it('seçilen integer kimliği ve açıklamayı gönderir', async () => {
    await open();
    await fireEvent.press(screen.getByText('Hukuk'));
    await fireEvent.changeText(screen.getByPlaceholderText('Açıklamanızı yazın'), '  İncelensin  ');
    await fireEvent.press(screen.getByText('İşlemi onayla'));
    expect(mutateAsync).toHaveBeenCalledWith({ action: 'DEPARTMANA_GONDER', targetDepartmentId: 12, comment: 'İncelensin' });
  });
  it('action değişince hedefi temizler ve hedefsiz aksiyona kimlik eklemez', async () => {
    await open();
    await fireEvent.press(screen.getByText('Hukuk'));
    await fireEvent.press(screen.getByText('Vazgeç'));
    await fireEvent.press(screen.getByText(departmentAction.displayName));
    await fireEvent.press(screen.getByText('İşlemi onayla'));
    expect(mutateAsync).not.toHaveBeenCalled();
    await fireEvent.press(screen.getByText('Vazgeç'));
    await fireEvent.press(screen.getByText('Son onay'));
    await fireEvent.press(screen.getByText('İşlemi onayla'));
    expect(mutateAsync).toHaveBeenCalledWith({ action: 'ONAYLA' });
  });
  it.each([
    [{ isPending: true }, 'Departmanlar yükleniyor…'],
    [{ isFetching: true }, 'Departmanlar yükleniyor…'],
    [{ isError: true }, 'Departmanlar yüklenemedi.'],
    [{ data: { departments: [] } }, 'Gönderilebilecek departman yok.'],
  ])('seçenekler kullanılamazken göndermez: %s', async (state, message) => {
    targetState(state);
    await open();
    expect(screen.getByText(message)).toBeTruthy();
    await fireEvent.press(screen.getByText('İşlemi onayla'));
    expect(mutateAsync).not.toHaveBeenCalled();
  });
  it('seçenek hatasını yeniden deneyebilir', async () => {
    targetState({ isError: true });
    await open();
    await fireEvent.press(screen.getByText('Departmanları yeniden yükle'));
    expect(refetch).toHaveBeenCalledTimes(1);
  });
  it('yenilenen listeden çıkarılan eski hedefi göndermez', async () => {
    const view = await render(<RecordWorkflowActions record={mockRecord} />, { wrapper: createWrapper() });
    await fireEvent.press(screen.getByText(departmentAction.displayName));
    await fireEvent.press(screen.getByText('Hukuk'));
    targetState({ data: { departments: [options[1]] } });
    await view.rerender(<RecordWorkflowActions record={mockRecord} />);
    await fireEvent.press(screen.getByText('İşlemi onayla'));
    expect(mutateAsync).not.toHaveBeenCalled();
  });
});
