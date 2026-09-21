import { performWorkflowAction } from './workflow';

function jsonResponse(body: unknown): Response {
  return {
    headers: { get: () => 'application/json' },
    ok: true,
    status: 200,
    text: jest.fn().mockResolvedValue(JSON.stringify(body)),
  } as unknown as Response;
}

describe('workflow action response assignment ve version sözleşmesi', () => {
  it('ortak assignment modelini ve integer version alanını korur', async () => {
    const fetchMock = jest.fn().mockResolvedValue(
      jsonResponse({
        action: 'DEPARTMANA_GONDER',
        assignedTo: null,
        assignment: {
          departmentId: 12,
          departmentName: 'Hukuk',
          kind: 'DEPARTMENT',
          userFullName: null,
          userId: null,
        },
        newStatus: 'BSK_YRD_INCELEMESINDE',
        performedAt: '2026-09-14T10:30:00Z',
        performedBy: 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
        previousStatus: 'TASLAK',
        recordId: 'd3b07384-d113-4632-8fe2-51a6597a7a58',
        version: 8,
      }),
    );
    globalThis.fetch = fetchMock as typeof fetch;

    const result = await performWorkflowAction(
      'd3b07384-d113-4632-8fe2-51a6597a7a58',
      { action: 'DEPARTMANA_GONDER', targetDepartmentId: 12 },
    );

    expect(result.assignment).toEqual(
      expect.objectContaining({ kind: 'DEPARTMENT', departmentName: 'Hukuk' }),
    );
    expect(result.version).toBe(8);
  });
});
