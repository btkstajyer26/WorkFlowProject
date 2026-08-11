import { AdminController } from './generated/AdminController'
import { AuditLogController } from './generated/AuditLogController'
import { AuthController } from './generated/AuthController'
import { CategoryController } from './generated/CategoryController'
import { FileController } from './generated/FileController'
import { HttpClient } from './generated/http-client'
import { RecordController } from './generated/RecordController'
import { WorkflowActionController } from './generated/WorkflowActionController'
import { apiBaseUrl } from './config'
import { toApiClientError } from './errors'

type ApiSecurityData = {
  accessToken: string
}

export const apiHttpClient = new HttpClient<ApiSecurityData>({
  baseURL: apiBaseUrl,
  securityWorker: (securityData) => securityData
    ? { headers: { Authorization: `Bearer ${securityData.accessToken}` } }
    : undefined,
})

apiHttpClient.instance.interceptors.response.use(
  (response) => response,
  (error: unknown) => Promise.reject(toApiClientError(error)),
)

export const api = {
  admin: new AdminController(apiHttpClient),
  auditLogs: new AuditLogController(apiHttpClient),
  auth: new AuthController(apiHttpClient),
  categories: new CategoryController(apiHttpClient),
  files: new FileController(apiHttpClient),
  records: new RecordController(apiHttpClient),
  workflow: new WorkflowActionController(apiHttpClient),
}

export function setApiAccessToken(accessToken: string) {
  apiHttpClient.setSecurityData({ accessToken })
}

export function clearApiAccessToken() {
  apiHttpClient.setSecurityData(null)
}
