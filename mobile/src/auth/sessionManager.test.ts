import {
  changePassword,
  login,
  logout,
  refreshSession,
  type LoginResponse,
} from '@/api/auth';
import {
  clearSessionTokens,
  getRefreshToken,
  saveSessionTokens,
} from './tokenStore';
import {
  endSession,
  restoreSession,
  startSession,
  subscribeToSession,
  updatePassword,
} from './sessionManager';

jest.mock('@/api/auth', () => ({
  changePassword: jest.fn(),
  login: jest.fn(),
  logout: jest.fn(),
  refreshSession: jest.fn(),
}));

jest.mock('@/api/client', () => ({
  setApiAuthHandlers: jest.fn(),
}));

jest.mock('./tokenStore', () => ({
  clearSessionTokens: jest.fn(),
  getAccessToken: jest.fn(),
  getRefreshToken: jest.fn(),
  saveSessionTokens: jest.fn(),
}));

const session: LoginResponse = {
  accessToken: 'access-token',
  mustChangePassword: false,
  refreshToken: 'rotated-refresh-token',
};

const loginMock = jest.mocked(login);
const logoutMock = jest.mocked(logout);
const refreshSessionMock = jest.mocked(refreshSession);
const changePasswordMock = jest.mocked(changePassword);
const clearSessionTokensMock = jest.mocked(clearSessionTokens);
const getRefreshTokenMock = jest.mocked(getRefreshToken);
const saveSessionTokensMock = jest.mocked(saveSessionTokens);

describe('sessionManager', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    getRefreshTokenMock.mockResolvedValue('stored-refresh-token');
  });

  it('girişte dönen tokenları saklar ve oturumu yayınlar', async () => {
    const listener = jest.fn();
    const unsubscribe = subscribeToSession(listener);
    loginMock.mockResolvedValue(session);

    await expect(
      startSession({ email: 'test@example.com', password: '12345678a' }),
    ).resolves.toEqual(session);

    expect(saveSessionTokensMock).toHaveBeenCalledWith(session);
    expect(listener).toHaveBeenCalledWith({ mustChangePassword: false });
    unsubscribe();
  });

  it('eşzamanlı oturum yenilemelerinde backend çağrısını tekilleştirir', async () => {
    let resolveRefresh: (value: LoginResponse) => void = () => undefined;
    refreshSessionMock.mockReturnValue(
      new Promise<LoginResponse>((resolve) => {
        resolveRefresh = resolve;
      }),
    );

    const firstRestore = restoreSession();
    const secondRestore = restoreSession();

    await Promise.resolve();
    await Promise.resolve();
    expect(refreshSessionMock).toHaveBeenCalledTimes(1);
    resolveRefresh(session);

    await expect(Promise.all([firstRestore, secondRestore])).resolves.toEqual([
      session,
      session,
    ]);
    expect(saveSessionTokensMock).toHaveBeenCalledTimes(1);
  });

  it('refresh başarısız olduğunda yerel tokenları temizler', async () => {
    refreshSessionMock.mockRejectedValue(new Error('refresh failed'));

    await expect(restoreSession()).resolves.toBeNull();

    expect(clearSessionTokensMock).toHaveBeenCalledTimes(1);
  });

  it('logout sırasında opsiyonel cihaz tokenını backend gövdesine ekler', async () => {
    logoutMock.mockResolvedValue('Çıkış yapıldı');

    await endSession({ deviceToken: 'fcm-device-token' });

    expect(logoutMock).toHaveBeenCalledWith({
      deviceToken: 'fcm-device-token',
      refreshToken: 'stored-refresh-token',
    });
    expect(clearSessionTokensMock).toHaveBeenCalledTimes(1);
  });

  it('logout isteği başarısız olsa da cihazdaki oturumu temizler', async () => {
    logoutMock.mockRejectedValue(new Error('network failed'));

    await expect(endSession()).resolves.toBeUndefined();

    expect(clearSessionTokensMock).toHaveBeenCalledTimes(1);
  });

  it('parola değişiminden sonra oturumu kapatır', async () => {
    changePasswordMock.mockResolvedValue('Şifre değiştirildi');

    await updatePassword({
      currentPassword: 'old-password1',
      newPassword: 'new-password1',
    });

    expect(clearSessionTokensMock).toHaveBeenCalledTimes(1);
  });
});
