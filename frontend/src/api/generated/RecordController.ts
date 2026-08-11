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
  CreateRecordData,
  DeleteRecordData,
  DeleteRecordParams,
  GetAllRecordsData,
  GetAllRecordsParams,
  GetRecordByIdData,
  GetRecordByIdParams,
  RecordCreateRequest,
  RecordUpdateRequest,
  UpdateRecordData,
  UpdateRecordParams,
} from "./data-contracts";
import { HttpClient } from "./http-client";
import type { RequestParams } from "./http-client";

export class RecordController<SecurityDataType = unknown> {
  http: HttpClient<SecurityDataType>;

  constructor(http: HttpClient<SecurityDataType>) {
    this.http = http;
  }

  /**
   * No description
   *
   * @tags record-controller
   * @name CreateRecord
   * @request POST:/api/v1/records
   * @secure
   */
  createRecord = (data: RecordCreateRequest, params: RequestParams = {}) =>
    this.http.request<CreateRecordData, any>({
      path: `/api/v1/records`,
      method: "POST",
      body: data,
      secure: true,
      type: "application/json",
      ...params,
    });
  /**
   * No description
   *
   * @tags record-controller
   * @name DeleteRecord
   * @request DELETE:/api/v1/records/{id}
   * @secure
   */
  deleteRecord = ({ id }: DeleteRecordParams, params: RequestParams = {}) =>
    this.http.request<DeleteRecordData, any>({
      path: `/api/v1/records/${id}`,
      method: "DELETE",
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags record-controller
   * @name GetAllRecords
   * @request GET:/api/v1/records
   * @secure
   */
  getAllRecords = (query: GetAllRecordsParams, params: RequestParams = {}) =>
    this.http.request<GetAllRecordsData, any>({
      path: `/api/v1/records`,
      method: "GET",
      query: query,
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags record-controller
   * @name GetRecordById
   * @request GET:/api/v1/records/{id}
   * @secure
   */
  getRecordById = ({ id }: GetRecordByIdParams, params: RequestParams = {}) =>
    this.http.request<GetRecordByIdData, any>({
      path: `/api/v1/records/${id}`,
      method: "GET",
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags record-controller
   * @name UpdateRecord
   * @request PUT:/api/v1/records/{id}
   * @secure
   */
  updateRecord = (
    { id }: UpdateRecordParams,
    data: RecordUpdateRequest,
    params: RequestParams = {},
  ) =>
    this.http.request<UpdateRecordData, any>({
      path: `/api/v1/records/${id}`,
      method: "PUT",
      body: data,
      secure: true,
      type: "application/json",
      ...params,
    });
}
