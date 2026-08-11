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

export interface AuditLogResponse {
  action?: string;
  comment?: string;
  /** @format date-time */
  createdAt?: string;
  /** @format uuid */
  id?: string;
  newStatus?: string;
  previousStatus?: string;
  /** @format uuid */
  recordId?: string;
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

export type CreateRecordData = RecordResponse;

export type CreateUserData = UserResponse;

export interface CreateUserRequest {
  email?: string;
  firstName?: string;
  lastName?: string;
  password?: string;
  roleName?: string;
}

export type DeleteFileData = object;

export interface DeleteFileParams {
  /** @format uuid */
  deletedBy: string;
  /** @format uuid */
  id: string;
}

export type DeleteRecordData = any;

export interface DeleteRecordParams {
  /** @format uuid */
  id: string;
}

/** @format binary */
export type DownloadFileData = File;

export interface DownloadFileParams {
  /** @format uuid */
  id: string;
}

export type GetAllCategoriesData = CategoryResponse[];

export type GetAllRecordsData = PageRecordResponse;

export interface GetAllRecordsParams {
  /** @format int32 */
  categoryId?: number;
  keyword?: string;
  pageable: Pageable;
  status?:
    | "TASLAK"
    | "BSK_YRD_INCELEMESINDE"
    | "BASKAN_INCELEMESINDE"
    | "DUZENLEME_BEKLIYOR"
    | "ONAYLANDI"
    | "REDDEDILDI";
}

export type GetGecmisData = AuditLogResponse[];

export interface GetGecmisParams {
  /** @format uuid */
  recordId: string;
}

export type GetRecordByIdData = RecordResponse;

export interface GetRecordByIdParams {
  /** @format uuid */
  id: string;
}

export type LoginData = LoginResponse;

export interface LoginRequest {
  email?: string;
  password?: string;
}

export interface LoginResponse {
  accessToken?: string;
  refreshToken?: string;
}

export type LogoutData = string;

export interface LogoutRequest {
  refreshToken?: string;
}

export interface PageRecordResponse {
  content?: RecordResponse[];
  empty?: boolean;
  first?: boolean;
  last?: boolean;
  /** @format int32 */
  number?: number;
  /** @format int32 */
  numberOfElements?: number;
  pageable?: PageableObject;
  /** @format int32 */
  size?: number;
  sort?: SortObject;
  /** @format int64 */
  totalElements?: number;
  /** @format int32 */
  totalPages?: number;
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

export interface PageableObject {
  /** @format int64 */
  offset?: number;
  /** @format int32 */
  pageNumber?: number;
  /** @format int32 */
  pageSize?: number;
  paged?: boolean;
  sort?: SortObject;
  unpaged?: boolean;
}

export type PerformActionData = WorkflowActionResponse;

export interface PerformActionParams {
  /** @format uuid */
  recordId: string;
}

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
  refreshToken?: string;
}

export interface SortObject {
  empty?: boolean;
  sorted?: boolean;
  unsorted?: boolean;
}

export type UpdateRecordData = RecordResponse;

export interface UpdateRecordParams {
  /** @format uuid */
  id: string;
}

export type UploadFileData = object;

export interface UploadFileParams {
  /** @format uuid */
  recordId: string;
  /** @format uuid */
  uploadedBy: string;
}

export interface UploadFilePayload {
  /** @format binary */
  file: File;
}

export interface UserResponse {
  /** @format date-time */
  createdAt?: string;
  email?: string;
  firstName?: string;
  /** @format uuid */
  id?: string;
  lastName?: string;
  roleName?: string;
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
