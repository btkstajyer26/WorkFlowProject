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

import type {
  GetRolePermissionsData,
  GetRolePermissionsParams,
  ListPermissionsData,
  UpdateRolePermissionsData,
  UpdateRolePermissionsParams,
  UpdateRolePermissionsRequest,
} from "./data-contracts";
import { HttpClient } from "./http-client";
import type { RequestParams } from "./http-client";

export class PermissionAdminController<SecurityDataType = unknown> {
  http: HttpClient<SecurityDataType>;

  constructor(http: HttpClient<SecurityDataType>) {
    this.http = http;
  }

  /**
   * No description
   *
   * @tags permission-admin-controller
   * @name GetRolePermissions
   * @request GET:/api/admin/roles/{id}/permissions
   * @secure
   */
  getRolePermissions = (
    { id }: GetRolePermissionsParams,
    params: RequestParams = {},
  ) =>
    this.http.request<GetRolePermissionsData, any>({
      path: `/api/admin/roles/${id}/permissions`,
      method: "GET",
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags permission-admin-controller
   * @name ListPermissions
   * @request GET:/api/admin/permissions
   * @secure
   */
  listPermissions = (params: RequestParams = {}) =>
    this.http.request<ListPermissionsData, any>({
      path: `/api/admin/permissions`,
      method: "GET",
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags permission-admin-controller
   * @name UpdateRolePermissions
   * @request PUT:/api/admin/roles/{id}/permissions
   * @secure
   */
  updateRolePermissions = (
    { id }: UpdateRolePermissionsParams,
    data: UpdateRolePermissionsRequest,
    params: RequestParams = {},
  ) =>
    this.http.request<UpdateRolePermissionsData, any>({
      path: `/api/admin/roles/${id}/permissions`,
      method: "PUT",
      body: data,
      secure: true,
      type: "application/json",
      ...params,
    });
}
