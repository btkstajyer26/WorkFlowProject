import { isAxiosError, type InternalAxiosRequestConfig } from 'axios'
import { AdminController } from './generated/AdminController'
import { AuditLogController } from './generated/AuditLogController'
import { AuthController } from './generated/AuthController'
import { CategoryController } from './generated/CategoryController'
import { FileController } from './generated/FileController'
import { HttpClient } from './generated/http-client'
import { NotificationController } from './generated/NotificationController'
import { PermissionAdminController } from './generated/PermissionAdminController'
import { RecordController } from './generated/RecordController'
import { RoleAdminController } from './generated/RoleAdminController'
import { UserController } from './generated/UserController'
import { WorkflowActionController } from './generated/WorkflowActionController'
import { WorkflowActorBindings } from './generated/WorkflowActorBindings'
import { WorkflowQueryController } from './generated/WorkflowQueryController'
import { apiBaseUrl } from './config'
import { toApiClientError } from './errors'

type ApiSecurityData = {
  accessToken: string
}

type RetriableRequestConfig = InternalAxiosRequestConfig & {
  _ebysAuthRetry?: boolean
}

type AccessTokenRefresher = () => Promise<string>

let refreshAccessToken: AccessTokenRefresher | null = null

function isAuthEndpoint(url?: string) {
  return Boolean(url?.includes('/api/auth/'))
}

export const apiHttpClient = new HttpClient<ApiSecurityData>({
  baseURL: apiBaseUrl,
  securityWorker: (securityData) => securityData
    ? { headers: { Authorization: `Bearer ${securityData.accessToken}` } }
    : undefined,
})

apiHttpClient.instance.interceptors.response.use(
  (response) => response,
  async (error: unknown) => {
    if (
      isAxiosError(error) &&
      error.response?.status === 401 &&
      error.config &&
      !isAuthEndpoint(error.config.url) &&
      !(error.config as RetriableRequestConfig)._ebysAuthRetry &&
      refreshAccessToken
    ) {
      const request = error.config as RetriableRequestConfig
      request._ebysAuthRetry = true

      try {
        const accessToken = await refreshAccessToken()
        request.headers.set('Authorization', `Bearer ${accessToken}`)
        return apiHttpClient.instance.request(request)
      } catch {
        // Yenileme hatası auth katmanında oturumu temizler. İstemciye ilk
        // 401 yanıtını ortak ApiClientError sözleşmesiyle iletiriz.
      }
    }

    return Promise.reject(toApiClientError(error))
  },
)

export const api = {
  admin: new AdminController(apiHttpClient),
  auditLogs: new AuditLogController(apiHttpClient),
  auth: new AuthController(apiHttpClient),
  categories: new CategoryController(apiHttpClient),
  files: new FileController(apiHttpClient),
  notifications: new NotificationController(apiHttpClient),
  // AP-3 rol <-> permission matrisi.
  permissions: new PermissionAdminController(apiHttpClient),
  records: new RecordController(apiHttpClient),
  roles: new RoleAdminController(apiHttpClient),
  users: new UserController(apiHttpClient),
  workflow: new WorkflowActionController(apiHttpClient),
  // AP-8 aktor-rol baglama yonetimi.
  workflowActorBindings: new WorkflowActorBindings(apiHttpClient),
  // APP-9 okuma uclari: yetkili aksiyonlar ve hedef departman kesfi.
  workflowQuery: new WorkflowQueryController(apiHttpClient),
}

export function setApiAccessToken(accessToken: string) {
  apiHttpClient.setSecurityData({ accessToken })
}

export function clearApiAccessToken() {
  apiHttpClient.setSecurityData(null)
}

export function setApiAccessTokenRefresher(refresher: AccessTokenRefresher) {
  refreshAccessToken = refresher
}
