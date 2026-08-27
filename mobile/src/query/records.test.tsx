import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react-native';
import type { PropsWithChildren } from 'react';

import { getRecords, type RecordPage, type RecordStatus } from '@/api/records';

import { useInfiniteRecords } from './records';

jest.mock('@/api/records', () => ({
  ...jest.requireActual('@/api/records'),
  getRecords: jest.fn(),
}));

const mockedGetRecords = jest.mocked(getRecords);

function createPage(status: RecordStatus, title: string, createdAt: string): RecordPage {
  return {
    content: [
      {
        assignedTo: null,
        categoryId: 1,
        createdAt,
        createdBy: '00000000-0000-4000-8000-000000000001',
        createdByFullName: 'Test Kullanıcı',
        description: `${title} açıklaması`,
        id:
          status === 'ONAYLANDI'
            ? '00000000-0000-4000-8000-000000000002'
            : '00000000-0000-4000-8000-000000000003',
        status,
        title,
        updatedAt: createdAt,
      },
    ],
    page: 0,
    size: 2,
    totalElements: 1,
    totalPages: 1,
  };
}

describe('useInfiniteRecords', () => {
  it('birden fazla durum görünümünü ayrı sorgulayıp tarihe göre birleştirir', async () => {
    mockedGetRecords.mockImplementation(async ({ status }) => {
      if (status === 'ONAYLANDI') {
        return createPage('ONAYLANDI', 'Onaylanan kayıt', '2026-08-20T10:00:00');
      }

      return createPage('REDDEDILDI', 'Reddedilen kayıt', '2026-08-21T10:00:00');
    });

    const queryClient = new QueryClient({
      defaultOptions: { queries: { gcTime: Infinity, retry: false } },
    });
    const wrapper = ({ children }: PropsWithChildren) => (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );
    const { result, unmount } = await renderHook(
      () =>
        useInfiniteRecords({
          size: 2,
          sort: 'createdAt,desc',
          statuses: ['ONAYLANDI', 'REDDEDILDI'],
        }),
      { wrapper },
    );

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(mockedGetRecords).toHaveBeenCalledTimes(2);
    expect(mockedGetRecords).toHaveBeenCalledWith(
      expect.objectContaining({ page: 0, size: 2, status: 'ONAYLANDI' }),
    );
    expect(mockedGetRecords).toHaveBeenCalledWith(
      expect.objectContaining({ page: 0, size: 2, status: 'REDDEDILDI' }),
    );
    expect(result.current.data?.pages[0]?.content.map((record) => record.status)).toEqual([
      'REDDEDILDI',
      'ONAYLANDI',
    ]);
    expect(result.current.data?.pages[0]?.totalElements).toBe(2);

    await unmount();
    await queryClient.cancelQueries();
    queryClient.clear();
  });
});
