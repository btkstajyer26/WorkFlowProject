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

import {
  DeviceTokenDeleteRequest,
  DeviceTokenRequest,
  RegisterTokenData,
  RemoveTokenData,
} from "./data-contracts";
import { HttpClient, RequestParams } from "./http-client";

export class DeviceTokens<SecurityDataType = unknown> {
  http: HttpClient<SecurityDataType>;

  constructor(http: HttpClient<SecurityDataType>) {
    this.http = http;
  }

  /**
   * No description
   *
   * @tags Device Tokens
   * @name RegisterToken
   * @summary Cihaz token kaydı / güncelleme (Upsert)
   * @request POST:/api/device-tokens
   * @secure
   */
  registerToken = (data: DeviceTokenRequest, params: RequestParams = {}) =>
    this.http.request<RegisterTokenData, any>({
      path: `/api/device-tokens`,
      method: "POST",
      body: data,
      secure: true,
      type: "application/json",
      ...params,
    });
  /**
   * No description
   *
   * @tags Device Tokens
   * @name RemoveToken
   * @summary Cihaz tokenını pasifleştir (Sahiplik doğrulamalı)
   * @request DELETE:/api/device-tokens
   * @secure
   */
  removeToken = (data: DeviceTokenDeleteRequest, params: RequestParams = {}) =>
    this.http.request<RemoveTokenData, any>({
      path: `/api/device-tokens`,
      method: "DELETE",
      body: data,
      secure: true,
      type: "application/json",
      ...params,
    });
}
