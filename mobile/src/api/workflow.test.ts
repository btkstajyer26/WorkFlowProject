import { apiRequest } from './client';
import { getAvailableWorkflowActions, getWorkflowTargetDepartments, performWorkflowAction } from './workflow';

jest.mock('./client', () => ({ apiRequest: jest.fn() }));
const request = jest.mocked(apiRequest);
const recordId = 'd3b07384-d113-4632-8fe2-51a6597a7a58';

beforeEach(() => jest.resetAllMocks());

it('kayıt kapsamlı departman seçeneklerini ayrıştırır', async () => {
  request.mockResolvedValue({ departments: [{ id: 12, name: 'Hukuk' }] });
  await expect(getWorkflowTargetDepartments(recordId)).resolves.toEqual({ departments: [{ id: 12, name: 'Hukuk' }] });
  expect(request).toHaveBeenCalledWith(`/api/records/${recordId}/workflow/target-departments`);
});

it('boş departman listesi geçerlidir', async () => {
  request.mockResolvedValue({ departments: [] });
  await expect(getWorkflowTargetDepartments(recordId)).resolves.toEqual({ departments: [] });
});

it.each(['12', 1.5, null])('geçersiz departman kimliğini reddeder: %s', async (id) => {
  request.mockResolvedValue({ departments: [{ id, name: 'Hukuk' }] });
  await expect(getWorkflowTargetDepartments(recordId)).rejects.toThrow();
});

it('backend aksiyon bayraklarını ve gösterim adını korur', async () => {
  const response = { recordId, status: 'TASLAK', version: 2, actions: [{
    action: 'DEPARTMANA_GONDER', displayName: 'Birime ilet', commentRequired: true,
    targetDepartmentRequired: true, targetUserRequired: false,
  }] };
  request.mockResolvedValue(response);
  await expect(getAvailableWorkflowActions(recordId)).resolves.toEqual(response);
});

it('POST sözleşmesinde departman kimliğini gönderir, version eklemez', async () => {
  request.mockResolvedValue({ action: 'DEPARTMANA_GONDER', newStatus: 'BSK_YRD_INCELEMESINDE',
    previousStatus: 'TASLAK', recordId, performedBy: recordId, performedAt: '2026-09-08T12:00:00' });
  await performWorkflowAction(recordId, { action: 'DEPARTMANA_GONDER', targetDepartmentId: 12 });
  expect(request).toHaveBeenCalledWith(`/api/records/${recordId}/workflow/actions`, {
    json: { action: 'DEPARTMANA_GONDER', targetDepartmentId: 12 }, method: 'POST',
  });
});
