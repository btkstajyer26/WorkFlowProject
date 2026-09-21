import { getRecord, getRecords } from './records';

function jsonResponse(body: unknown): Response {
  return {
    headers: { get: () => 'application/json' },
    ok: true,
    status: 200,
    text: jest.fn().mockResolvedValue(JSON.stringify(body)),
  } as unknown as Response;
}

const recordResponse = {
  categoryId: 1,
  createdAt: '2026-09-14T10:00:00Z',
  createdBy: 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
  createdByFullName: 'Ahmet Yılmaz',
  description: 'Sözleşme incelemesi',
  id: 'd3b07384-d113-4632-8fe2-51a6597a7a58',
  status: 'BSK_YRD_INCELEMESINDE',
  title: 'Hukuk görüşü',
  version: 7,
};

describe('records API assignment ve version sözleşmesi', () => {
  const fetchMock = jest.fn();

  beforeEach(() => {
    fetchMock.mockReset();
    globalThis.fetch = fetchMock as typeof fetch;
  });

  it('detay yanıtında assignment, kind ve version alanlarını korur', async () => {
    fetchMock.mockResolvedValue(
      jsonResponse({
        ...recordResponse,
        assignment: {
          departmentId: 12,
          departmentName: 'Hukuk',
          kind: 'DEPARTMENT',
          userFullName: null,
          userId: null,
        },
      }),
    );

    const result = await getRecord(recordResponse.id);

    expect(result.assignment).toEqual({
      departmentId: 12,
      departmentName: 'Hukuk',
      kind: 'DEPARTMENT',
      userFullName: null,
      userId: null,
    });
    expect(result.assignment?.kind).toBe('DEPARTMENT');
    expect(result.version).toBe(7);
  });

  it.each([
    ['null', null],
    ['absent', undefined],
  ])('assignment %s iken yanıtı güvenle parse eder', async (_case, assignment) => {
    fetchMock.mockResolvedValue(
      jsonResponse({
        ...recordResponse,
        ...(assignment === undefined ? {} : { assignment }),
      }),
    );

    const result = await getRecord(recordResponse.id);

    expect(result.assignment).toBe(assignment);
    expect(result.version).toBe(7);
  });

  it('liste satırında kullanıcı assignment ve version alanlarını korur', async () => {
    fetchMock.mockResolvedValue(
      jsonResponse({
        content: [
          {
            ...recordResponse,
            assignedTo: '00000000-0000-4000-8000-000000000001',
            assignment: {
              departmentId: null,
              departmentName: null,
              kind: 'USER',
              userFullName: 'Ayşe Demir',
              userId: '00000000-0000-4000-8000-000000000001',
            },
            updatedAt: '2026-09-14T11:00:00Z',
          },
        ],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
      }),
    );

    const result = await getRecords({ page: 0, size: 20 });

    expect(result.content[0]).toEqual(
      expect.objectContaining({
        assignment: expect.objectContaining({
          kind: 'USER',
          userFullName: 'Ayşe Demir',
        }),
        version: 7,
      }),
    );
  });
});
