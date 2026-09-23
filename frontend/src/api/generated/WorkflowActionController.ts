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
  PerformAction1Data,
  PerformAction1Params,
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
   * @name PerformAction1
   * @request POST:/api/records/{recordId}/workflow/actions
   * @secure
   */
  performAction1 = (
    { recordId }: PerformAction1Params,
    data: WorkflowActionRequest,
    params: RequestParams = {},
  ) =>
    this.http.request<PerformAction1Data, any>({
      path: `/api/records/${recordId}/workflow/actions`,
      method: "POST",
      body: data,
      secure: true,
      type: "application/json",
      ...params,
    });
}
