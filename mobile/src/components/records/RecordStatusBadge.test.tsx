import { render, screen } from '@testing-library/react-native';

import { RecordStatusBadge } from './RecordStatusBadge';

describe('RecordStatusBadge', () => {
  it.each([
    ['TASLAK', 'Taslak'],
    ['BSK_YRD_INCELEMESINDE', 'Bşk. Yrd. incelemesinde'],
    ['BASKAN_INCELEMESINDE', 'Başkan incelemesinde'],
    ['DUZENLEME_BEKLIYOR', 'Düzenleme bekliyor'],
    ['ONAYLANDI', 'Onaylandı'],
    ['REDDEDILDI', 'Reddedildi'],
  ] as const)('%s durumu için "%s" etiketini basar', async (status, expectedLabel) => {
    await render(<RecordStatusBadge status={status} />);
    expect(screen.getByText(expectedLabel)).toBeTruthy();
  });
});


