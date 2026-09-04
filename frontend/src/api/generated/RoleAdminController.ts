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
  CreateRoleData,
  CreateRoleRequest,
  ListRolesData,
  ListRolesParams,
  UpdateRoleData,
  UpdateRoleParams,
  UpdateRoleRequest,
} from "./data-contracts";
import { HttpClient } from "./http-client";
import type { RequestParams } from "./http-client";

export class RoleAdminController<SecurityDataType = unknown> {
  http: HttpClient<SecurityDataType>;

  constructor(http: HttpClient<SecurityDataType>) {
    this.http = http;
  }

  /**
   * No description
   *
   * @tags role-admin-controller
   * @name CreateRole
   * @request POST:/api/admin/roles
   * @secure
   */
  createRole = (data: CreateRoleRequest, params: RequestParams = {}) =>
    this.http.request<CreateRoleData, any>({
      path: `/api/admin/roles`,
      method: "POST",
      body: data,
      secure: true,
      type: "application/json",
      ...params,
    });
  /**
   * No description
   *
   * @tags role-admin-controller
   * @name ListRoles
   * @request GET:/api/admin/roles
   * @secure
   */
  listRoles = (query: ListRolesParams = {}, params: RequestParams = {}) =>
    this.http.request<ListRolesData, any>({
      path: `/api/admin/roles`,
      method: "GET",
      query: query,
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags role-admin-controller
   * @name UpdateRole
   * @request PATCH:/api/admin/roles/{id}
   * @secure
   */
  updateRole = (
    { id }: UpdateRoleParams,
    data: UpdateRoleRequest,
    params: RequestParams = {},
  ) =>
    this.http.request<UpdateRoleData, any>({
      path: `/api/admin/roles/${id}`,
      method: "PATCH",
      body: data,
      secure: true,
      type: "application/json",
      ...params,
    });
}
