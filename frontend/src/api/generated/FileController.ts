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
  DeleteFileData,
  DeleteFileParams,
  DownloadFileData,
  DownloadFileParams,
  PreviewFileData,
  PreviewFileParams,
  UploadFileData,
  UploadFileParams,
  UploadFilePayload,
} from "./data-contracts";
import { HttpClient } from "./http-client";
import type { RequestParams } from "./http-client";

export class FileController<SecurityDataType = unknown> {
  http: HttpClient<SecurityDataType>;

  constructor(http: HttpClient<SecurityDataType>) {
    this.http = http;
  }

  /**
   * No description
   *
   * @tags file-controller
   * @name DeleteFile
   * @request DELETE:/api/files/{id}
   * @secure
   */
  deleteFile = (
    { id, ...query }: DeleteFileParams,
    params: RequestParams = {},
  ) =>
    this.http.request<DeleteFileData, any>({
      path: `/api/files/${id}`,
      method: "DELETE",
      query: query,
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags file-controller
   * @name DownloadFile
   * @request GET:/api/files/{id}/download
   * @secure
   */
  downloadFile = ({ id }: DownloadFileParams, params: RequestParams = {}) =>
    this.http.request<DownloadFileData, any>({
      path: `/api/files/${id}/download`,
      method: "GET",
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags file-controller
   * @name PreviewFile
   * @request GET:/api/files/{id}/preview
   * @secure
   */
  previewFile = ({ id }: PreviewFileParams, params: RequestParams = {}) =>
    this.http.request<PreviewFileData, any>({
      path: `/api/files/${id}/preview`,
      method: "GET",
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags file-controller
   * @name UploadFile
   * @request POST:/api/files/upload
   * @secure
   */
  uploadFile = (
    query: UploadFileParams,
    data: UploadFilePayload,
    params: RequestParams = {},
  ) =>
    this.http.request<UploadFileData, any>({
      path: `/api/files/upload`,
      method: "POST",
      query: query,
      body: data,
      secure: true,
      type: "multipart/form-data",
      ...params,
    });
}
