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
  AddDepartmentMemberRequest,
  AddMemberData,
  AddMemberParams,
  CreateDepartmentData,
  CreateDepartmentRequest,
  ListDepartmentsData,
  ListDepartmentsParams,
  ListMembersData,
  ListMembersParams,
  RemoveMemberData,
  RemoveMemberParams,
  UpdateDepartmentData,
  UpdateDepartmentParams,
  UpdateDepartmentRequest,
} from "./data-contracts";
import { HttpClient } from "./http-client";
import type { RequestParams } from "./http-client";

export class DepartmentAdminController<SecurityDataType = unknown> {
  http: HttpClient<SecurityDataType>;

  constructor(http: HttpClient<SecurityDataType>) {
    this.http = http;
  }

  /**
   * No description
   *
   * @tags department-admin-controller
   * @name AddMember
   * @request POST:/api/admin/departments/{id}/members
   * @secure
   */
  addMember = (
    { id }: AddMemberParams,
    data: AddDepartmentMemberRequest,
    params: RequestParams = {},
  ) =>
    this.http.request<AddMemberData, any>({
      path: `/api/admin/departments/${id}/members`,
      method: "POST",
      body: data,
      secure: true,
      type: "application/json",
      ...params,
    });
  /**
   * No description
   *
   * @tags department-admin-controller
   * @name CreateDepartment
   * @request POST:/api/admin/departments
   * @secure
   */
  createDepartment = (
    data: CreateDepartmentRequest,
    params: RequestParams = {},
  ) =>
    this.http.request<CreateDepartmentData, any>({
      path: `/api/admin/departments`,
      method: "POST",
      body: data,
      secure: true,
      type: "application/json",
      ...params,
    });
  /**
   * No description
   *
   * @tags department-admin-controller
   * @name ListDepartments
   * @request GET:/api/admin/departments
   * @secure
   */
  listDepartments = (
    query: ListDepartmentsParams = {},
    params: RequestParams = {},
  ) =>
    this.http.request<ListDepartmentsData, any>({
      path: `/api/admin/departments`,
      method: "GET",
      query: query,
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags department-admin-controller
   * @name ListMembers
   * @request GET:/api/admin/departments/{id}/members
   * @secure
   */
  listMembers = ({ id }: ListMembersParams, params: RequestParams = {}) =>
    this.http.request<ListMembersData, any>({
      path: `/api/admin/departments/${id}/members`,
      method: "GET",
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags department-admin-controller
   * @name RemoveMember
   * @request DELETE:/api/admin/departments/{id}/members/{userId}
   * @secure
   */
  removeMember = (
    { id, userId }: RemoveMemberParams,
    params: RequestParams = {},
  ) =>
    this.http.request<RemoveMemberData, any>({
      path: `/api/admin/departments/${id}/members/${userId}`,
      method: "DELETE",
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags department-admin-controller
   * @name UpdateDepartment
   * @request PATCH:/api/admin/departments/{id}
   * @secure
   */
  updateDepartment = (
    { id }: UpdateDepartmentParams,
    data: UpdateDepartmentRequest,
    params: RequestParams = {},
  ) =>
    this.http.request<UpdateDepartmentData, any>({
      path: `/api/admin/departments/${id}`,
      method: "PATCH",
      body: data,
      secure: true,
      type: "application/json",
      ...params,
    });
}
