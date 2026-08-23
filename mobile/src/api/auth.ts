import { apiRequest } from './client';

const AUTH_BASE_PATH = '/api/auth';

export type LoginRequest = {
  email: string;
  password: string;
};

export type LoginResponse = {
  accessToken: string;
  mustChangePassword: boolean;
  refreshToken: string;
};

export type RefreshTokenRequest = {
  refreshToken: string;
};

export type LogoutRequest = RefreshTokenRequest & {
  deviceToken?: string;
};

export type ChangePasswordRequest = {
  currentPassword: string;
  newPassword: string;
};

export type ForgotPasswordRequest = {
  email: string;
};

export type VerifyResetCodeRequest = {
  code: string;
  email: string;
};

export type VerifyResetCodeResponse = {
  expiresInSeconds: number;
  resetToken: string;
};

export type ResetPasswordRequest = {
  newPassword: string;
  token: string;
};

export function login(request: LoginRequest): Promise<LoginResponse> {
  return apiRequest(`${AUTH_BASE_PATH}/login`, {
    auth: false,
    json: request,
    method: 'POST',
  });
}

export function refreshSession(request: RefreshTokenRequest): Promise<LoginResponse> {
  return apiRequest(`${AUTH_BASE_PATH}/refresh`, {
    auth: false,
    json: request,
    method: 'POST',
  });
}

export function logout(request: LogoutRequest): Promise<string> {
  return apiRequest(`${AUTH_BASE_PATH}/logout`, {
    auth: false,
    json: request,
    method: 'POST',
  });
}

export function changePassword(
  request: ChangePasswordRequest,
): Promise<string> {
  return apiRequest(`${AUTH_BASE_PATH}/change-password`, {
    json: request,
    method: 'POST',
  });
}

export function forgotPassword(request: ForgotPasswordRequest): Promise<void> {
  return apiRequest(`${AUTH_BASE_PATH}/forgot-password`, {
    auth: false,
    json: request,
    method: 'POST',
  });
}

export function verifyResetCode(
  request: VerifyResetCodeRequest,
): Promise<VerifyResetCodeResponse> {
  return apiRequest(`${AUTH_BASE_PATH}/verify-reset-code`, {
    auth: false,
    json: request,
    method: 'POST',
  });
}

export function resetPassword(request: ResetPasswordRequest): Promise<void> {
  return apiRequest(`${AUTH_BASE_PATH}/reset-password`, {
    auth: false,
    json: request,
    method: 'POST',
  });
}
