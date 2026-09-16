import { render, screen } from '@testing-library/react-native';

import { RecordAssignment } from './RecordAssignment';

describe('RecordAssignment', () => {
  it('departman atamasını backend gösterim adıyla gösterir', async () => {
    await render(
      <RecordAssignment
        assignment={{
          departmentId: 12,
          departmentName: 'Hukuk',
          kind: 'DEPARTMENT',
        }}
      />,
    );

    expect(screen.getByText('Atanan departman')).toBeTruthy();
    expect(screen.getByText('Hukuk')).toBeTruthy();
  });

  it('kullanıcı atamasını backend gösterim adıyla gösterir', async () => {
    await render(
      <RecordAssignment
        assignment={{
          kind: 'USER',
          userFullName: 'Ayşe Demir',
          userId: 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
        }}
      />,
    );

    expect(screen.getByText('Atanan kullanıcı')).toBeTruthy();
    expect(screen.getByText('Ayşe Demir')).toBeTruthy();
  });

  it.each([undefined, null, { kind: 'NONE' } as const])(
    'atama yokken boş bir alan göstermez: %s',
    async (assignment) => {
      const { toJSON } = await render(<RecordAssignment assignment={assignment} />);

      expect(toJSON()).toBeNull();
      expect(screen.queryByText(/Atanan/)).toBeNull();
    },
  );
});
