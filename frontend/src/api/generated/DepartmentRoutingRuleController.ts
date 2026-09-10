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
  CreateDepartmentRoutingRuleRequest,
  CreateRuleData,
  CreateRuleParams,
  ListRulesData,
  ListRulesParams,
  UpdateDepartmentRoutingRuleRequest,
  UpdateRuleData,
  UpdateRuleParams,
} from "./data-contracts";
import { HttpClient } from "./http-client";
import type { RequestParams } from "./http-client";

export class DepartmentRoutingRuleController<SecurityDataType = unknown> {
  http: HttpClient<SecurityDataType>;

  constructor(http: HttpClient<SecurityDataType>) {
    this.http = http;
  }

  /**
   * No description
   *
   * @tags department-routing-rule-controller
   * @name CreateRule
   * @request POST:/api/admin/departments/{departmentId}/routing-rules
   * @secure
   */
  createRule = (
    { departmentId }: CreateRuleParams,
    data: CreateDepartmentRoutingRuleRequest,
    params: RequestParams = {},
  ) =>
    this.http.request<CreateRuleData, any>({
      path: `/api/admin/departments/${departmentId}/routing-rules`,
      method: "POST",
      body: data,
      secure: true,
      type: "application/json",
      ...params,
    });
  /**
   * No description
   *
   * @tags department-routing-rule-controller
   * @name ListRules
   * @request GET:/api/admin/departments/{departmentId}/routing-rules
   * @secure
   */
  listRules = ({ departmentId }: ListRulesParams, params: RequestParams = {}) =>
    this.http.request<ListRulesData, any>({
      path: `/api/admin/departments/${departmentId}/routing-rules`,
      method: "GET",
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags department-routing-rule-controller
   * @name UpdateRule
   * @request PATCH:/api/admin/departments/{departmentId}/routing-rules/{ruleId}
   * @secure
   */
  updateRule = (
    { departmentId, ruleId }: UpdateRuleParams,
    data: UpdateDepartmentRoutingRuleRequest,
    params: RequestParams = {},
  ) =>
    this.http.request<UpdateRuleData, any>({
      path: `/api/admin/departments/${departmentId}/routing-rules/${ruleId}`,
      method: "PATCH",
      body: data,
      secure: true,
      type: "application/json",
      ...params,
    });
}
