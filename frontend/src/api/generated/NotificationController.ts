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
  CountUnreadData,
  GetAllData,
  GetAllParams,
  GetUnreadData,
  MarkAsReadData,
  MarkAsReadParams,
} from "./data-contracts";
import { HttpClient } from "./http-client";
import type { RequestParams } from "./http-client";

export class NotificationController<SecurityDataType = unknown> {
  http: HttpClient<SecurityDataType>;

  constructor(http: HttpClient<SecurityDataType>) {
    this.http = http;
  }

  /**
   * No description
   *
   * @tags notification-controller
   * @name CountUnread
   * @request GET:/api/notifications/unread/count
   * @secure
   */
  countUnread = (params: RequestParams = {}) =>
    this.http.request<CountUnreadData, any>({
      path: `/api/notifications/unread/count`,
      method: "GET",
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags notification-controller
   * @name GetAll
   * @request GET:/api/notifications
   * @secure
   */
  getAll = (query: GetAllParams, params: RequestParams = {}) =>
    this.http.request<GetAllData, any>({
      path: `/api/notifications`,
      method: "GET",
      query: query,
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags notification-controller
   * @name GetUnread
   * @request GET:/api/notifications/unread
   * @secure
   */
  getUnread = (params: RequestParams = {}) =>
    this.http.request<GetUnreadData, any>({
      path: `/api/notifications/unread`,
      method: "GET",
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags notification-controller
   * @name MarkAsRead
   * @request PUT:/api/notifications/{id}/read
   * @secure
   */
  markAsRead = ({ id }: MarkAsReadParams, params: RequestParams = {}) =>
    this.http.request<MarkAsReadData, any>({
      path: `/api/notifications/${id}/read`,
      method: "PUT",
      secure: true,
      ...params,
    });
}
