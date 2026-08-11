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

import type { CreateUserData, CreateUserRequest } from "./data-contracts";
import { HttpClient } from "./http-client";
import type { RequestParams } from "./http-client";

export class AdminController<SecurityDataType = unknown> {
  http: HttpClient<SecurityDataType>;

  constructor(http: HttpClient<SecurityDataType>) {
    this.http = http;
  }

  /**
   * No description
   *
   * @tags admin-controller
   * @name CreateUser
   * @request POST:/api/admin/users
   * @secure
   */
  createUser = (data: CreateUserRequest, params: RequestParams = {}) =>
    this.http.request<CreateUserData, any>({
      path: `/api/admin/users`,
      method: "POST",
      body: data,
      secure: true,
      type: "application/json",
      ...params,
    });
}
