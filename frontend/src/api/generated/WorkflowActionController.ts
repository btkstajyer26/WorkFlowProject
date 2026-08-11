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
  PerformActionData,
  PerformActionParams,
  WorkflowActionRequest,
} from "./data-contracts";
import { HttpClient } from "./http-client";
import type { RequestParams } from "./http-client";

export class WorkflowActionController<SecurityDataType = unknown> {
  http: HttpClient<SecurityDataType>;

  constructor(http: HttpClient<SecurityDataType>) {
    this.http = http;
  }

  /**
   * No description
   *
   * @tags workflow-action-controller
   * @name PerformAction
   * @request POST:/api/records/{recordId}/workflow/actions
   * @secure
   */
  performAction = (
    { recordId }: PerformActionParams,
    data: WorkflowActionRequest,
    params: RequestParams = {},
  ) =>
    this.http.request<PerformActionData, any>({
      path: `/api/records/${recordId}/workflow/actions`,
      method: "POST",
      body: data,
      secure: true,
      type: "application/json",
      ...params,
    });
}
