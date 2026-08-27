import { isNetworkOnline } from './networkStatus';

describe('isNetworkOnline', () => {
  it('bağlantı ve internet erişimi varsa çevrimiçi kabul eder', () => {
    expect(
      isNetworkOnline({ isConnected: true, isInternetReachable: true }),
    ).toBe(true);
  });

  it('internet erişimi henüz belirlenmediyse aktif bağlantıyı çevrimiçi kabul eder', () => {
    expect(
      isNetworkOnline({ isConnected: true, isInternetReachable: undefined }),
    ).toBe(true);
  });

  it.each([
    { isConnected: false, isInternetReachable: false },
    { isConnected: true, isInternetReachable: false },
    { isConnected: undefined, isInternetReachable: undefined },
  ])('erişilemeyen ağı çevrimdışı kabul eder: %o', (state) => {
    expect(isNetworkOnline(state)).toBe(false);
  });
});
