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
  ConsumeData,
  MailActionTokenRequest,
  PreviewData,
} from "./data-contracts";
import { HttpClient, RequestParams } from "./http-client";

export class MailActions<SecurityDataType = unknown> {
  http: HttpClient<SecurityDataType>;

  constructor(http: HttpClient<SecurityDataType>) {
    this.http = http;
  }

  /**
   * No description
   *
   * @tags Mail Actions
   * @name Consume
   * @summary Baglantiyi tuket ve workflow aksiyonunu yurut
   * @request POST:/api/public/mail-actions/consume
   * @secure
   */
  consume = (data: MailActionTokenRequest, params: RequestParams = {}) =>
    this.http.request<ConsumeData, any>({
      path: `/api/public/mail-actions/consume`,
      method: "POST",
      body: data,
      secure: true,
      type: "application/json",
      ...params,
    });
  /**
   * No description
   *
   * @tags Mail Actions
   * @name Preview
   * @summary Baglantiyi dogrula ve onay ekrani bilgisini getir (durum degistirmez)
   * @request POST:/api/public/mail-actions/preview
   * @secure
   */
  preview = (data: MailActionTokenRequest, params: RequestParams = {}) =>
    this.http.request<PreviewData, any>({
      path: `/api/public/mail-actions/preview`,
      method: "POST",
      body: data,
      secure: true,
      type: "application/json",
      ...params,
    });
}
