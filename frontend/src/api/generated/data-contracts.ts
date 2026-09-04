/* eslint-disable */
/* tslint:disable */
// @ts-nocheck
/*
 * ---------------------------------------------------------------
 * ## THIS FILE WAS GENERATED VIA SWAGGER-TYPESCRIPT-API        ##
 * ##                                                           ##
 * ## AUTHOR: acacode                                           ##
 * ## SOURCE: https://github.com/acacode/swagger-typescript-api ##
 * ---------------------------------------------------------------
 */

export interface AdminUserSearchCriteria {
  active?: boolean;
  q?: string;
  role?: string;
}

export interface AuditLogResponse {
  action?: string;
  comment?: string;
  /** @format date-time */
  createdAt?: string;
  errorCode?: string;
  httpMethod?: string;
  /** @format int32 */
  httpStatus?: number;
  /** @format uuid */
  id?: string;
  newStatus?: string;
  previousStatus?: string;
  /** @format uuid */
  recordId?: string;
  requestPath?: string;
  /** @format int32 */
  roleId?: number;
  roleName?: string;
  userFullName?: string;
  /** @format uuid */
  userId?: string;
}

export interface CategoryResponse {
  /** @format int32 */
  id?: number;
  name?: string;
}

export type ChangePasswordData = string;

export interface ChangePasswordRequest {
  /** @minLength 1 */
  currentPassword: string;
  /**
   * @minLength 1
   * @pattern ^(?=.*[A-Za-z])(?=.*\d).{8,}$
   */
  newPassword: string;
}

export type ChangeRoleData = UserResponse;

export interface ChangeRoleParams {
  /** @format uuid */
  id: string;
}

/** roleId (pozitif) veya eski roleName alanlarından tam biri zorunludur. İkisi birlikte veya ikisi de eksikse 400 döner. */
export interface ChangeRoleRequest {
  /** @format uuid */
  replacementBaskanYardimcisiId?: string;
  /**
   * Atanacak rolün kimliği; roleName ile birlikte gönderilemez
   * @format int32
   * @min 1
   */
  roleId?: number;
  /**
   * Eski istemciler için rol adı; roleId ile birlikte gönderilemez
   * @deprecated
   */
  roleName?: string;
}

export type ConsumeData = Record<string, string>;

/** @format int64 */
export type CountUnreadData = number;

export type CreateRecordData = RecordResponse;

export type CreateRoleData = RoleResponse;

export interface CreateRoleRequest {
  /**
   * @minLength 0
   * @maxLength 255
   */
  description?: string;
  /**
   * @minLength 0
   * @maxLength 100
   */
  name: string;
  workflowActor?: boolean;
}

export type CreateUserData = UserResponse;

export interface CreateUserRequest {
  /**
   * @format email
   * @minLength 1
   */
  email: string;
  /** @minLength 1 */
  firstName: string;
  /** @minLength 1 */
  lastName: string;
  /**
   * @minLength 6
   * @maxLength 2147483647
   */
  password: string;
}

export type DeleteFileData = any;

export interface DeleteFileParams {
  /** @format uuid */
  id: string;
}

export type DeleteRecordData = any;

export interface DeleteRecordParams {
  /** @format uuid */
  id: string;
}

export interface DeviceTokenDeleteRequest {
  /** @minLength 1 */
  token: string;
}

export interface DeviceTokenRequest {
  deviceName?: string;
  /**
   * @minLength 1
   * @pattern ^(ANDROID|IOS)$
   */
  platform: string;
  /** @minLength 1 */
  token: string;
}

/** @format binary */
export type DownloadFileData = File;

export interface DownloadFileParams {
  /** @format uuid */
  id: string;
}

export interface FileResponseDto {
  /** @format int32 */
  fileSize?: number;
  /** @format uuid */
  id?: string;
  mimeType?: string;
  originalName?: string;
  /** @format uuid */
  recordId?: string;
  /** @format date-time */
  uploadedAt?: string;
  /** @format uuid */
  uploadedBy?: string;
}

export type ForgotPasswordData = any;

export interface ForgotPasswordRequest {
  /**
   * @format email
   * @minLength 1
   */
  email: string;
}

export type GetAllCategoriesData = CategoryResponse[];

export type GetAllData = PagedResponseNotificationResponse;

export interface GetAllParams {
  pageable: Pageable;
}

export type GetAllRecordsData = PagedResponseRecordSearchResponse;

export interface GetAllRecordsParams {
  /** @format int32 */
  categoryId?: number;
  creator?: string;
  /** @format date-time */
  from?: string;
  pageable: Pageable;
  q?: string;
  status?:
    | "TASLAK"
    | "BSK_YRD_INCELEMESINDE"
    | "BASKAN_INCELEMESINDE"
    | "DUZENLEME_BEKLIYOR"
    | "ONAYLANDI"
    | "REDDEDILDI";
  /** @format date-time */
  to?: string;
}

export type GetGecmis1Data = AuditLogResponse[];

export interface GetGecmis1Params {
  /** @format uuid */
  recordId: string;
}

export type GetGecmisData = UserAuditLogResponse[];

export interface GetGecmisParams {
  /** @format uuid */
  targetUserId: string;
}

export type GetRecordByIdData = RecordResponse;

export interface GetRecordByIdParams {
  /** @format uuid */
  id: string;
}

export type GetUnreadData = NotificationResponse[];

export type ListAuditLogsData = PagedResponseObject;

export interface ListAuditLogsParams {
  pageable: Pageable;
  /** @default "USER" */
  type?: string;
}

export type ListFilesData = FileResponseDto[];

export interface ListFilesParams {
  /** @format uuid */
  id: string;
}

export type ListRolesData = RoleResponse[];

export interface ListRolesParams {
  /** @default false */
  includeInactive?: boolean;
}

export type ListUsersData = PagedResponseUserResponse;

export interface ListUsersParams {
  criteria: AdminUserSearchCriteria;
  pageable: Pageable;
}

export type LoginData = LoginResponse;

export interface LoginRequest {
  /**
   * @format email
   * @minLength 1
   */
  email: string;
  /** @minLength 1 */
  password: string;
}

export interface LoginResponse {
  accessToken?: string;
  mustChangePassword?: boolean;
  refreshToken?: string;
}

export type LogoutData = string;

export interface LogoutRequest {
  deviceToken?: string;
  /** @minLength 1 */
  refreshToken: string;
}

export interface MailActionPreview {
  action?: string;
  /** @format date-time */
  expiresAt?: string;
  recipientName?: string;
  /** @format uuid */
  recordId?: string;
  recordStatus?: string;
  recordTitle?: string;
}

export interface MailActionTokenRequest {
  /** @minLength 1 */
  token: string;
}

export type MarkAsReadData = any;

export interface MarkAsReadParams {
  /** @format uuid */
  id: string;
}

export type MeData = UserResponse;

export interface NotificationResponse {
  /** @format date-time */
  createdAt?: string;
  /** @format uuid */
  id?: string;
  message?: string;
  notificationType?:
    | "RECORD_SUBMITTED"
    | "RECORD_FORWARDED"
    | "RECORD_APPROVED"
    | "RECORD_REJECTED"
    | "RECORD_RETURNED";
  read?: boolean;
  /** @format uuid */
  recordId?: string;
}

export interface Pageable {
  /**
   * @format int32
   * @min 0
   */
  page?: number;
  /**
   * @format int32
   * @min 1
   */
  size?: number;
  sort?: string[];
}

export interface PagedResponseNotificationResponse {
  content?: NotificationResponse[];
  /** @format int32 */
  page?: number;
  /** @format int32 */
  size?: number;
  /** @format int64 */
  totalElements?: number;
  /** @format int32 */
  totalPages?: number;
}

export interface PagedResponseObject {
  content?: any[];
  /** @format int32 */
  page?: number;
  /** @format int32 */
  size?: number;
  /** @format int64 */
  totalElements?: number;
  /** @format int32 */
  totalPages?: number;
}

export interface PagedResponseRecordSearchResponse {
  content?: RecordSearchResponse[];
  /** @format int32 */
  page?: number;
  /** @format int32 */
  size?: number;
  /** @format int64 */
  totalElements?: number;
  /** @format int32 */
  totalPages?: number;
}

export interface PagedResponseUserResponse {
  content?: UserResponse[];
  /** @format int32 */
  page?: number;
  /** @format int32 */
  size?: number;
  /** @format int64 */
  totalElements?: number;
  /** @format int32 */
  totalPages?: number;
}

export type PerformActionData = WorkflowActionResponse;

export interface PerformActionParams {
  /** @format uuid */
  recordId: string;
}

export type PreviewData = MailActionPreview;

/** @format binary */
export type PreviewFileData = File;

export interface PreviewFileParams {
  /** @format uuid */
  id: string;
}

export interface RecordCreateRequest {
  /** @format int32 */
  categoryId: number;
  /** @minLength 1 */
  description: string;
  /** @minLength 1 */
  title: string;
}

export interface RecordResponse {
  /** @format int32 */
  categoryId?: number;
  /** @format date-time */
  createdAt?: string;
  /** @format uuid */
  createdBy?: string;
  createdByFullName?: string;
  description?: string;
  /** @format uuid */
  id?: string;
  status?:
    | "TASLAK"
    | "BSK_YRD_INCELEMESINDE"
    | "BASKAN_INCELEMESINDE"
    | "DUZENLEME_BEKLIYOR"
    | "ONAYLANDI"
    | "REDDEDILDI";
  title?: string;
}

export interface RecordSearchResponse {
  /** @format uuid */
  assignedTo?: string;
  /** @format int32 */
  categoryId?: number;
  /** @format date-time */
  createdAt?: string;
  /** @format uuid */
  createdBy?: string;
  createdByFullName?: string;
  description?: string;
  /** @format uuid */
  id?: string;
  status?:
    | "TASLAK"
    | "BSK_YRD_INCELEMESINDE"
    | "BASKAN_INCELEMESINDE"
    | "DUZENLEME_BEKLIYOR"
    | "ONAYLANDI"
    | "REDDEDILDI";
  title?: string;
  /** @format date-time */
  updatedAt?: string;
}

export interface RecordUpdateRequest {
  /** @format int32 */
  categoryId: number;
  /** @minLength 1 */
  description: string;
  /** @minLength 1 */
  title: string;
}

export type RefreshData = LoginResponse;

export interface RefreshTokenRequest {
  /** @minLength 1 */
  refreshToken: string;
}

export type RegisterTokenData = any;

export type ReloadData = Record<string, number>;

export type RemoveTokenData = any;

export type ResetPasswordData = any;

export interface ResetPasswordRequest {
  /**
   * @minLength 1
   * @pattern ^(?=.*[A-Za-z])(?=.*\d).{8,}$
   */
  newPassword: string;
  /** @minLength 1 */
  token: string;
}

export interface RoleResponse {
  active?: boolean;
  description?: string;
  /** @format int32 */
  id?: number;
  /** @format int32 */
  maxUsers?: number;
  name?: string;
  system?: boolean;
  systemKey?: string;
  workflowActor?: boolean;
}

export type SetActiveData = UserResponse;

export interface SetActiveParams {
  /** @format uuid */
  id: string;
}

export interface SetActiveRequest {
  active: boolean;
}

export type UpdateRecordData = RecordResponse;

export interface UpdateRecordParams {
  /** @format uuid */
  id: string;
}

export type UpdateRoleData = RoleResponse;

export interface UpdateRoleParams {
  /** @format int32 */
  id: number;
}

export interface UpdateRoleRequest {
  active?: boolean;
  /**
   * @minLength 0
   * @maxLength 255
   */
  description?: string;
  /**
   * @minLength 0
   * @maxLength 100
   */
  name?: string;
  workflowActor?: boolean;
}

export type UploadFilesData = FileResponseDto[];

export interface UploadFilesParams {
  /** @format uuid */
  id: string;
}

export interface UploadFilesPayload {
  file: File[];
}

export interface UserAuditLogResponse {
  action?: string;
  comment?: string;
  /** @format date-time */
  createdAt?: string;
  errorCode?: string;
  httpMethod?: string;
  /** @format int32 */
  httpStatus?: number;
  /** @format uuid */
  id?: string;
  newActive?: boolean;
  /** @format int32 */
  newRoleId?: number;
  newRoleName?: string;
  /** @format uuid */
  performedBy?: string;
  performedByFullName?: string;
  previousActive?: boolean;
  /** @format int32 */
  previousRoleId?: number;
  previousRoleName?: string;
  requestPath?: string;
  targetUserFullName?: string;
  /** @format uuid */
  targetUserId?: string;
}

export interface UserResponse {
  active?: boolean;
  /** @format date-time */
  createdAt?: string;
  email?: string;
  firstName?: string;
  /** @format uuid */
  id?: string;
  lastName?: string;
  /** @format int32 */
  roleId?: number;
  roleName?: string;
  systemKey?: string;
}

export type VerifyResetCodeData = VerifyResetCodeResponse;

export interface VerifyResetCodeRequest {
  /**
   * @minLength 1
   * @pattern ^\d{6}$
   */
  code: string;
  /**
   * @format email
   * @minLength 1
   */
  email: string;
}

export interface VerifyResetCodeResponse {
  /** @format int64 */
  expiresInSeconds?: number;
  resetToken?: string;
}

export interface WorkflowActionRequest {
  action:
    | "GONDER"
    | "TEKRAR_GONDER"
    | "BASKANA_ILET"
    | "CALISANA_GERI_GONDER"
    | "BASKAN_YARDIMCISINA_GERI_GONDER"
    | "ONAYLA"
    | "REDDET";
  /**
   * @minLength 0
   * @maxLength 2000
   */
  comment?: string;
  /** @format uuid */
  targetUserId?: string;
}

export interface WorkflowActionResponse {
  action?:
    | "GONDER"
    | "TEKRAR_GONDER"
    | "BASKANA_ILET"
    | "CALISANA_GERI_GONDER"
    | "BASKAN_YARDIMCISINA_GERI_GONDER"
    | "ONAYLA"
    | "REDDET";
  /** @format uuid */
  assignedTo?: string;
  newStatus?:
    | "TASLAK"
    | "BSK_YRD_INCELEMESINDE"
    | "BASKAN_INCELEMESINDE"
    | "DUZENLEME_BEKLIYOR"
    | "ONAYLANDI"
    | "REDDEDILDI";
  /** @format date-time */
  performedAt?: string;
  /** @format uuid */
  performedBy?: string;
  previousStatus?:
    | "TASLAK"
    | "BSK_YRD_INCELEMESINDE"
    | "BASKAN_INCELEMESINDE"
    | "DUZENLEME_BEKLIYOR"
    | "ONAYLANDI"
    | "REDDEDILDI";
  /** @format uuid */
  recordId?: string;
}
