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
  BindActorRequest,
  BindData,
  ListData,
  UnbindData,
  UnbindParams,
} from "./data-contracts";
import { HttpClient } from "./http-client";
import type { RequestParams } from "./http-client";

export class WorkflowActorBindings<SecurityDataType = unknown> {
  http: HttpClient<SecurityDataType>;

  constructor(http: HttpClient<SecurityDataType>) {
    this.http = http;
  }

  /**
   * No description
   *
   * @tags Workflow Actor Bindings
   * @name Bind
   * @summary Bir şablon geçişe dinamik bir rolü aktör olarak bağlar
   * @request POST:/api/workflow/actor-bindings
   * @secure
   */
  bind = (data: BindActorRequest, params: RequestParams = {}) =>
    this.http.request<BindData, any>({
      path: `/api/workflow/actor-bindings`,
      method: "POST",
      body: data,
      secure: true,
      type: "application/json",
      ...params,
    });
  /**
   * No description
   *
   * @tags Workflow Actor Bindings
   * @name List
   * @summary Aktif ve pasif tüm aktör-rol bağlarını listeler
   * @request GET:/api/workflow/actor-bindings
   * @secure
   */
  list = (params: RequestParams = {}) =>
    this.http.request<ListData, any>({
      path: `/api/workflow/actor-bindings`,
      method: "GET",
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Workflow Actor Bindings
   * @name Unbind
   * @summary Bir aktör-rol bağını pasifleştirir
   * @request DELETE:/api/workflow/actor-bindings/{bindingId}
   * @secure
   */
  unbind = ({ bindingId }: UnbindParams, params: RequestParams = {}) =>
    this.http.request<UnbindData, any>({
      path: `/api/workflow/actor-bindings/${bindingId}`,
      method: "DELETE",
      secure: true,
      ...params,
    });
}
