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

import { ReloadData } from "./data-contracts";
import { HttpClient, RequestParams } from "./http-client";

export class WorkflowRules<SecurityDataType = unknown> {
  http: HttpClient<SecurityDataType>;

  constructor(http: HttpClient<SecurityDataType>) {
    this.http = http;
  }

  /**
   * No description
   *
   * @tags Workflow Rules
   * @name Reload
   * @summary Gecis kurallarini veritabanindan yeniden okur (grafigi degistirmez)
   * @request POST:/api/workflow/rules/reload
   * @secure
   */
  reload = (params: RequestParams = {}) =>
    this.http.request<ReloadData, any>({
      path: `/api/workflow/rules/reload`,
      method: "POST",
      secure: true,
      ...params,
    });
}
