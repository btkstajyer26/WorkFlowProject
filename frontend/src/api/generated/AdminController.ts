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
  ChangeRoleData,
  ChangeRoleParams,
  ChangeRoleRequest,
  CreateUserData,
  CreateUserRequest,
  ListAuditLogsData,
  ListAuditLogsParams,
  ListRolesData,
  ListUsersData,
  ListUsersParams,
  SetActiveData,
  SetActiveParams,
  SetActiveRequest,
} from "./data-contracts";
import { HttpClient } from "./http-client";
import type { RequestParams } from "./http-client";

export class AdminController<SecurityDataType = unknown> {
  http: HttpClient<SecurityDataType>;

  constructor(http: HttpClient<SecurityDataType>) {
    this.http = http;
  }

  /**
   * No description
   *
   * @tags admin-controller
   * @name ChangeRole
   * @request PATCH:/api/admin/users/{id}/role
   * @secure
   */
  changeRole = (
    { id }: ChangeRoleParams,
    data: ChangeRoleRequest,
    params: RequestParams = {},
  ) =>
    this.http.request<ChangeRoleData, any>({
      path: `/api/admin/users/${id}/role`,
      method: "PATCH",
      body: data,
      secure: true,
      type: "application/json",
      ...params,
    });
  /**
   * No description
   *
   * @tags admin-controller
   * @name CreateUser
   * @request POST:/api/admin/users
   * @secure
   */
  createUser = (data: CreateUserRequest, params: RequestParams = {}) =>
    this.http.request<CreateUserData, any>({
      path: `/api/admin/users`,
      method: "POST",
      body: data,
      secure: true,
      type: "application/json",
      ...params,
    });
  /**
   * No description
   *
   * @tags admin-controller
   * @name ListAuditLogs
   * @request GET:/api/admin/audit-logs
   * @secure
   */
  listAuditLogs = (query: ListAuditLogsParams, params: RequestParams = {}) =>
    this.http.request<ListAuditLogsData, any>({
      path: `/api/admin/audit-logs`,
      method: "GET",
      query: query,
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags admin-controller
   * @name ListRoles
   * @request GET:/api/admin/roles
   * @secure
   */
  listRoles = (params: RequestParams = {}) =>
    this.http.request<ListRolesData, any>({
      path: `/api/admin/roles`,
      method: "GET",
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags admin-controller
   * @name ListUsers
   * @request GET:/api/admin/users
   * @secure
   */
  listUsers = (query: ListUsersParams, params: RequestParams = {}) =>
    this.http.request<ListUsersData, any>({
      path: `/api/admin/users`,
      method: "GET",
      query: query,
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags admin-controller
   * @name SetActive
   * @request PATCH:/api/admin/users/{id}/active
   * @secure
   */
  setActive = (
    { id }: SetActiveParams,
    data: SetActiveRequest,
    params: RequestParams = {},
  ) =>
    this.http.request<SetActiveData, any>({
      path: `/api/admin/users/${id}/active`,
      method: "PATCH",
      body: data,
      secure: true,
      type: "application/json",
      ...params,
    });
}
