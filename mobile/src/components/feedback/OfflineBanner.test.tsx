import { render, screen } from '@testing-library/react-native';

import { OfflineBanner } from './OfflineBanner';

describe('OfflineBanner', () => {
  it('çevrimdışı iken uyarı mesajını render eder', async () => {
    await render(<OfflineBanner isOffline={true} />);
    expect(
      screen.getByText(
        'İnternet bağlantısı yok. Bağlantı gelince veriler yenilenecek.',
      ),
    ).toBeTruthy();
  });

  it('çevrimiçi iken hiçbir şey render etmez', async () => {
    const { toJSON } = await render(<OfflineBanner isOffline={false} />);
    expect(toJSON()).toBeNull();
  });
});

