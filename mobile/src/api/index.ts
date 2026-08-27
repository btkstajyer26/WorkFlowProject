export {
  changePassword,
  forgotPassword,
  login,
  logout,
  refreshSession,
  resetPassword,
  verifyResetCode,
} from './auth';
export type {
  ChangePasswordRequest,
  ForgotPasswordRequest,
  LoginRequest,
  LoginResponse,
  LogoutRequest,
  RefreshTokenRequest,
  ResetPasswordRequest,
  VerifyResetCodeRequest,
  VerifyResetCodeResponse,
} from './auth';
export {
  API_BASE_URL,
  apiRequest,
  clearApiAuthHandlers,
  setApiAuthHandlers,
} from './client';
export type { ApiAuthHandlers, ApiRequestOptions } from './client';
export {
  deleteDeviceToken,
  devicePlatformSchema,
  deviceTokenRequestSchema,
  registerDeviceToken,
} from './deviceTokens';
export type { DevicePlatform, DeviceTokenRequest } from './deviceTokens';
export { ApiClientError } from './errors';
export type { ApiErrorBody, ApiFieldError } from './errors';
export {
  deleteRecordFile,
  downloadAndOpenFile,
  downloadFileToLocal,
  getRecordFiles,
  openOrShareFile,
  uploadRecordFile,
} from './files';
export type { RecordFile, ShareOptions } from './files';
export {
  getNotifications,
  getUnreadNotificationCount,
  getUnreadNotifications,
  markNotificationAsRead,
  notificationItemSchema,
  notificationPageSchema,
  notificationTypeSchema,
} from './notifications';
export type {
  NotificationItem,
  NotificationListQuery,
  NotificationPage,
  NotificationType,
} from './notifications';
export { getCurrentUser, userRoleSchema } from './users';
export type { CurrentUser, UserRole } from './users';
