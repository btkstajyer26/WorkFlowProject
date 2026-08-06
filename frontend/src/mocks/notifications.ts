import type { NotificationItem } from '../types/notification'

export const mockNotifications: NotificationItem[] = [
  {
    id: 'notification-001',
    userId: 'user-demo-001',
    recordId: 'rec-002',
    message: 'Yazılım Lisansı Talebi kaydınız eksik bilgi nedeniyle düzeltme için geri gönderildi.',
    isRead: false,
    createdAt: '2026-08-05T10:18:00',
  },
  {
    id: 'notification-002',
    userId: 'user-demo-001',
    recordId: 'rec-003',
    message: 'Toplantı Salonu Tadilat Talebi kaydınız Başkan incelemesine iletildi.',
    isRead: false,
    createdAt: '2026-08-05T09:42:00',
  },
  {
    id: 'notification-003',
    userId: 'user-demo-001',
    recordId: 'rec-004',
    message: 'Yıllık Bakım Sözleşmesi kaydınız Başkan tarafından onaylandı.',
    isRead: false,
    createdAt: '2026-08-04T16:24:00',
  },
  {
    id: 'notification-004',
    userId: 'user-demo-001',
    recordId: 'rec-001',
    message: 'Sunucu Donanım Alım Talebi kaydınız Başkan Yardımcısı incelemesine gönderildi.',
    isRead: false,
    createdAt: '2026-08-04T10:30:00',
  },
  {
    id: 'notification-005',
    userId: 'user-demo-001',
    recordId: 'rec-005',
    message: 'Kırtasiye Malzemesi Alımı kaydınız bütçe uygun olmadığı için reddedildi.',
    isRead: true,
    createdAt: '2026-08-01T09:20:00',
  },
  {
    id: 'notification-006',
    userId: 'user-demo-001',
    recordId: 'rec-009',
    message: 'Bütçe Aktarım Talebi kaydınız onaylanarak sonuçlandırıldı.',
    isRead: true,
    createdAt: '2026-07-28T17:10:00',
  },
]
