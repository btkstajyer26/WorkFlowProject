export { api, clearApiAccessToken, setApiAccessToken } from './client'
export { ApiClientError } from './errors'
export {
  getUnreadNotificationCount,
  listNotifications,
  listUnreadNotifications,
  markNotificationAsRead,
  type NotificationListQuery,
  type NotificationListItem,
  type NotificationListResult,
  type UnreadNotification,
} from './notifications'
export { listRecords, type RecordListQuery } from './records'
export {
  searchRecords,
  type RecordSearchListItem,
  type RecordSearchQuery,
  type RecordSearchResult,
} from './recordSearch'
export type * from './generated/data-contracts'
