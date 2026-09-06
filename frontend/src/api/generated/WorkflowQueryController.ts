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
  AvailableActionsData,
  AvailableActionsParams,
  TargetDepartmentsData,
  TargetDepartmentsParams,
} from "./data-contracts";
import { HttpClient } from "./http-client";
import type { RequestParams } from "./http-client";

export class WorkflowQueryController<SecurityDataType = unknown> {
  http: HttpClient<SecurityDataType>;

  constructor(http: HttpClient<SecurityDataType>) {
    this.http = http;
  }

  /**
   * No description
   *
   * @tags workflow-query-controller
   * @name AvailableActions
   * @request GET:/api/records/{recordId}/workflow/available-actions
   * @secure
   */
  availableActions = (
    { recordId }: AvailableActionsParams,
    params: RequestParams = {},
  ) =>
    this.http.request<AvailableActionsData, any>({
      path: `/api/records/${recordId}/workflow/available-actions`,
      method: "GET",
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags workflow-query-controller
   * @name TargetDepartments
   * @request GET:/api/records/{recordId}/workflow/target-departments
   * @secure
   */
  targetDepartments = (
    { recordId }: TargetDepartmentsParams,
    params: RequestParams = {},
  ) =>
    this.http.request<TargetDepartmentsData, any>({
      path: `/api/records/${recordId}/workflow/target-departments`,
      method: "GET",
      secure: true,
      ...params,
    });
}
