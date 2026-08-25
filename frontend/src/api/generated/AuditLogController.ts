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

import type { GetGecmis1Data, GetGecmis1Params } from "./data-contracts";
import { HttpClient } from "./http-client";
import type { RequestParams } from "./http-client";

export class AuditLogController<SecurityDataType = unknown> {
  http: HttpClient<SecurityDataType>;

  constructor(http: HttpClient<SecurityDataType>) {
    this.http = http;
  }

  /**
   * No description
   *
   * @tags audit-log-controller
   * @name GetGecmis1
   * @request GET:/api/audit-logs/record/{recordId}
   * @secure
   */
  getGecmis1 = ({ recordId }: GetGecmis1Params, params: RequestParams = {}) =>
    this.http.request<GetGecmis1Data, any>({
      path: `/api/audit-logs/record/${recordId}`,
      method: "GET",
      secure: true,
      ...params,
    });
}
