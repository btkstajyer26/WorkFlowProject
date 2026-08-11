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

import type { SearchData, SearchParams } from "./data-contracts";
import { HttpClient } from "./http-client";
import type { RequestParams } from "./http-client";

export class RecordSearchController<SecurityDataType = unknown> {
  http: HttpClient<SecurityDataType>;

  constructor(http: HttpClient<SecurityDataType>) {
    this.http = http;
  }

  /**
   * No description
   *
   * @tags record-search-controller
   * @name Search
   * @request GET:/api/records/search
   * @secure
   */
  search = (query: SearchParams, params: RequestParams = {}) =>
    this.http.request<SearchData, any>({
      path: `/api/records/search`,
      method: "GET",
      query: query,
      secure: true,
      ...params,
    });
}
