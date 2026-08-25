import { act, renderHook } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import type { ReactNode } from 'react'
import { describe, expect, it } from 'vitest'
import { api, setApiAccessToken } from '../api/client'
import { getDemoUserByRole } from '../mocks/users'
import { queryKeys } from '../query/queryKeys'
import type { WorkflowRecord } from '../types/record'
import { useRecordWorkflowAction } from './useRecordWorkflowAction'

describe('useRecordWorkflowAction', () => {
  it('çalışana geri gönderme sonucunu detay geçmişine hemen ekler', async () => {
    const tokens = await api.auth.login({ email: 'ayse.kaya@kurum.gov.tr', password: 'demo123' })
    setApiAccessToken(tokens.accessToken!)
    const actor = getDemoUserByRole('BASKAN_YARDIMCISI')
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
    const record: WorkflowRecord = {
      id: 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa2',
      recordNumber: '',
      title: 'Birim içi eğitim planı',
      description: 'Bilgi güvenliği eğitimi planıdır.',
      categoryId: 3,
      category: 'İnsan Kaynakları',
      status: 'BSK_YRD_INCELEMESINDE',
      createdBy: 'John Doe',
      assignedTo: 'Ayşe Kaya',
      lastAction: 'Başkan Yardımcısına gönderildi',
      createdAt: '2026-08-02T10:00:00Z',
      updatedAt: '2026-08-04T14:20:00Z',
      attachments: [],
      history: [],
    }
    queryClient.setQueryData(queryKeys.records.detail(record.id, '3:İnsan Kaynakları'), record)

    const wrapper = ({ children }: { children: ReactNode }) => (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    )
    const { result } = renderHook(() => useRecordWorkflowAction(record.id, actor), { wrapper })

    await act(() => result.current.mutateAsync({
      action: 'CALISANA_GERI_GONDER',
      comment: 'Belgeyi düzeltin.',
    }))

    const updatedRecord = queryClient.getQueryData<WorkflowRecord>(
      queryKeys.records.detail(record.id, '3:İnsan Kaynakları'),
    )
    expect(updatedRecord).toMatchObject({
      status: 'DUZENLEME_BEKLIYOR',
      lastAction: 'Çalışana geri gönderildi',
    })
    expect(updatedRecord?.history.at(-1)).toMatchObject({
      action: 'Çalışana geri gönderildi',
      actor: 'Ayşe Kaya',
      role: 'Başkan Yardımcısı',
      note: 'Belgeyi düzeltin.',
    })
  })
})
