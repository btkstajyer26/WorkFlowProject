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
  AssignableUsersData,
  AssignableUsersParams,
  List1Data,
  List1Params,
  PerformActionData,
  PerformActionParams,
  SplitData,
  SplitParams,
  SubtaskActionRequest,
  SubtaskSplitRequest,
} from "./data-contracts";
import { HttpClient } from "./http-client";
import type { RequestParams } from "./http-client";

export class SubtaskController<SecurityDataType = unknown> {
  http: HttpClient<SecurityDataType>;

  constructor(http: HttpClient<SecurityDataType>) {
    this.http = http;
  }

  /**
   * No description
   *
   * @tags subtask-controller
   * @name AssignableUsers
   * @request GET:/api/records/{recordId}/subtasks/assignable-users
   * @secure
   */
  assignableUsers = (
    { recordId }: AssignableUsersParams,
    params: RequestParams = {},
  ) =>
    this.http.request<AssignableUsersData, any>({
      path: `/api/records/${recordId}/subtasks/assignable-users`,
      method: "GET",
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags subtask-controller
   * @name List1
   * @request GET:/api/records/{recordId}/subtasks
   * @secure
   */
  list1 = ({ recordId }: List1Params, params: RequestParams = {}) =>
    this.http.request<List1Data, any>({
      path: `/api/records/${recordId}/subtasks`,
      method: "GET",
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags subtask-controller
   * @name PerformAction
   * @request POST:/api/subtasks/{subtaskId}/actions
   * @secure
   */
  performAction = (
    { subtaskId }: PerformActionParams,
    data: SubtaskActionRequest,
    params: RequestParams = {},
  ) =>
    this.http.request<PerformActionData, any>({
      path: `/api/subtasks/${subtaskId}/actions`,
      method: "POST",
      body: data,
      secure: true,
      type: "application/json",
      ...params,
    });
  /**
   * No description
   *
   * @tags subtask-controller
   * @name Split
   * @request POST:/api/records/{recordId}/subtasks/split
   * @secure
   */
  split = (
    { recordId }: SplitParams,
    data: SubtaskSplitRequest,
    params: RequestParams = {},
  ) =>
    this.http.request<SplitData, any>({
      path: `/api/records/${recordId}/subtasks/split`,
      method: "POST",
      body: data,
      secure: true,
      type: "application/json",
      ...params,
    });
}
