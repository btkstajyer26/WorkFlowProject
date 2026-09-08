import { act, renderHook, waitFor } from '@testing-library/react-native';

import { ApiClientError } from '@/api/errors';
import { getWorkflowTargetDepartments, performWorkflowAction } from '@/api/workflow';
import { createTestQueryClient, createWrapper } from '@/test-utils/testWrapper';
import { recordQueryKeys } from './records';
import { auditLogQueryKeys } from './auditLogs';
import { useRecordWorkflow, useWorkflowTargetDepartments, workflowQueryKeys } from './workflow';

jest.mock('@/api/workflow', () => ({
  getWorkflowTargetDepartments: jest.fn(),
  performWorkflowAction: jest.fn(),
}));
const recordId = 'd3b07384-d113-4632-8fe2-51a6597a7a58';
beforeEach(() => jest.resetAllMocks());

it('seçici açılmadan sorgulamaz; tekrar açılınca seçenekleri yeniler', async () => {
  jest.mocked(getWorkflowTargetDepartments).mockResolvedValue({ departments: [] });
  const client = createTestQueryClient();
  const hook = await renderHook<ReturnType<typeof useWorkflowTargetDepartments>, { enabled: boolean }>(({ enabled }) => useWorkflowTargetDepartments(recordId, enabled), {
    initialProps: { enabled: false }, wrapper: createWrapper(client),
  });
  expect(getWorkflowTargetDepartments).not.toHaveBeenCalled();
  await hook.rerender({ enabled: true });
  await waitFor(() => expect(hook.result.current.isSuccess).toBe(true));
  expect(getWorkflowTargetDepartments).toHaveBeenCalledWith(recordId);
  await hook.rerender({ enabled: false });
  await hook.rerender({ enabled: true });
  await waitFor(() => expect(getWorkflowTargetDepartments).toHaveBeenCalledTimes(2));
  await hook.unmount();
  await client.cancelQueries();
  client.clear();
});

it.each(['success', 'conflict'])('hedef seçeneklerini ve mevcut kayıt sorgularını yeniler: %s', async (outcome) => {
  const client = createTestQueryClient();
  client.setMutationDefaults([], { gcTime: Infinity });
  const invalidate = jest.spyOn(client, 'invalidateQueries');
  const error = new ApiClientError({ code: 'WORKFLOW_VERSION_CONFLICT', message: 'Yenileyin', status: 409 });
  if (outcome === 'success') jest.mocked(performWorkflowAction).mockResolvedValue({} as never);
  else jest.mocked(performWorkflowAction).mockRejectedValue(error);
  const hook = await renderHook(() => useRecordWorkflow(recordId), { wrapper: createWrapper(client) });
  await act(async () => {
    const result = hook.result.current.mutateAsync({ action: 'DEPARTMANA_GONDER', targetDepartmentId: 12 });
    if (outcome === 'success') await result;
    else await expect(result).rejects.toBe(error);
  });
  expect(invalidate).toHaveBeenCalledWith({ queryKey: workflowQueryKeys.targetDepartments(recordId) });
  expect(invalidate).toHaveBeenCalledWith({ queryKey: workflowQueryKeys.availableActions(recordId) });
  expect(invalidate).toHaveBeenCalledWith(expect.objectContaining({ queryKey: auditLogQueryKeys.record(recordId) }));
  expect(invalidate).toHaveBeenCalledWith(expect.objectContaining({
    queryKey: outcome === 'success' ? recordQueryKeys.lists() : recordQueryKeys.all,
  }));
  await hook.unmount();
  await client.cancelQueries();
  client.clear();
});
