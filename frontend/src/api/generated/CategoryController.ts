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

import type { GetAllCategoriesData } from "./data-contracts";
import { HttpClient } from "./http-client";
import type { RequestParams } from "./http-client";

export class CategoryController<SecurityDataType = unknown> {
  http: HttpClient<SecurityDataType>;

  constructor(http: HttpClient<SecurityDataType>) {
    this.http = http;
  }

  /**
   * No description
   *
   * @tags category-controller
   * @name GetAllCategories
   * @request GET:/api/categories
   * @secure
   */
  getAllCategories = (params: RequestParams = {}) =>
    this.http.request<GetAllCategoriesData, any>({
      path: `/api/categories`,
      method: "GET",
      secure: true,
      ...params,
    });
}
