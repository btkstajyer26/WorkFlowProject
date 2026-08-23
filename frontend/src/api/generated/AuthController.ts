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
  ChangePasswordData,
  ChangePasswordRequest,
  ForgotPasswordData,
  ForgotPasswordRequest,
  LoginData,
  LoginRequest,
  LogoutData,
  LogoutRequest,
  RefreshData,
  RefreshTokenRequest,
  ResetPasswordData,
  ResetPasswordRequest,
  VerifyResetCodeData,
  VerifyResetCodeRequest,
} from "./data-contracts";
import { HttpClient } from "./http-client";
import type { RequestParams } from "./http-client";

export class AuthController<SecurityDataType = unknown> {
  http: HttpClient<SecurityDataType>;

  constructor(http: HttpClient<SecurityDataType>) {
    this.http = http;
  }

  /**
   * No description
   *
   * @tags auth-controller
   * @name ChangePassword
   * @request POST:/api/auth/change-password
   * @secure
   */
  changePassword = (data: ChangePasswordRequest, params: RequestParams = {}) =>
    this.http.request<ChangePasswordData, any>({
      path: `/api/auth/change-password`,
      method: "POST",
      body: data,
      secure: true,
      type: "application/json",
      ...params,
    });
  /**
   * No description
   *
   * @tags auth-controller
   * @name ForgotPassword
   * @request POST:/api/auth/forgot-password
   * @secure
   */
  forgotPassword = (data: ForgotPasswordRequest, params: RequestParams = {}) =>
    this.http.request<ForgotPasswordData, any>({
      path: `/api/auth/forgot-password`,
      method: "POST",
      body: data,
      secure: true,
      type: "application/json",
      ...params,
    });
  /**
   * No description
   *
   * @tags auth-controller
   * @name Login
   * @request POST:/api/auth/login
   * @secure
   */
  login = (data: LoginRequest, params: RequestParams = {}) =>
    this.http.request<LoginData, any>({
      path: `/api/auth/login`,
      method: "POST",
      body: data,
      secure: true,
      type: "application/json",
      ...params,
    });
  /**
   * No description
   *
   * @tags auth-controller
   * @name Logout
   * @request POST:/api/auth/logout
   * @secure
   */
  logout = (data: LogoutRequest, params: RequestParams = {}) =>
    this.http.request<LogoutData, any>({
      path: `/api/auth/logout`,
      method: "POST",
      body: data,
      secure: true,
      type: "application/json",
      ...params,
    });
  /**
   * No description
   *
   * @tags auth-controller
   * @name Refresh
   * @request POST:/api/auth/refresh
   * @secure
   */
  refresh = (data: RefreshTokenRequest, params: RequestParams = {}) =>
    this.http.request<RefreshData, any>({
      path: `/api/auth/refresh`,
      method: "POST",
      body: data,
      secure: true,
      type: "application/json",
      ...params,
    });
  /**
   * No description
   *
   * @tags auth-controller
   * @name ResetPassword
   * @request POST:/api/auth/reset-password
   * @secure
   */
  resetPassword = (data: ResetPasswordRequest, params: RequestParams = {}) =>
    this.http.request<ResetPasswordData, any>({
      path: `/api/auth/reset-password`,
      method: "POST",
      body: data,
      secure: true,
      type: "application/json",
      ...params,
    });
  /**
   * No description
   *
   * @tags auth-controller
   * @name VerifyResetCode
   * @request POST:/api/auth/verify-reset-code
   * @secure
   */
  verifyResetCode = (
    data: VerifyResetCodeRequest,
    params: RequestParams = {},
  ) =>
    this.http.request<VerifyResetCodeData, any>({
      path: `/api/auth/verify-reset-code`,
      method: "POST",
      body: data,
      secure: true,
      type: "application/json",
      ...params,
    });
}
