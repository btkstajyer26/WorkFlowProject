import type { LucideIcon } from 'lucide-react'
import { Bell, FileClock, FolderKanban, LayoutDashboard, ShieldCheck, UsersRound } from 'lucide-react'
import type { UserRole } from '../types/auth'

export type RecordView = {
  label: string
  view?: string
}

export type PrimaryNavigationItem = {
  label: string
  to: string
  icon: LucideIcon
}

export const primaryNavigation: PrimaryNavigationItem[] = [
  {
    label: 'Dashboard',
    to: '/dashboard',
    icon: LayoutDashboard,
  },
]

export const adminNavigation: PrimaryNavigationItem[] = [
  {
    label: 'Yönetim Özeti',
    to: '/admin',
    icon: LayoutDashboard,
  },
  {
    label: 'Kullanıcılar',
    to: '/admin/kullanicilar',
    icon: UsersRound,
  },
  {
    label: 'Roller',
    to: '/admin/roller',
    icon: ShieldCheck,
  },
  {
    label: 'İşlem Kayıtları',
    to: '/admin/loglar',
    icon: FileClock,
  },
]

export const recordNavigation: Record<UserRole, RecordView[]> = {
  CALISAN: [
    { label: 'Tüm Kayıtlarım' },
    { label: 'Taslaklarım', view: 'taslaklar' },
    { label: 'Düzeltme Bekleyenler', view: 'duzeltme-bekleyenler' },
    { label: 'Onay Aşamasındakiler', view: 'onay-asamasindakiler' },
    { label: 'Sonuçlananlar', view: 'sonuclananlar' },
  ],
  BASKAN_YARDIMCISI: [
    { label: 'Tüm Kayıtlar' },
    { label: 'İncelenecekler', view: 'incelenecekler' },
    { label: 'Başkan İncelemesindekiler', view: 'baskan-incelemesindekiler' },
    { label: 'Düzeltmede Olanlar', view: 'duzeltmede-olanlar' },
    { label: 'Sonuçlananlar', view: 'sonuclananlar' },
  ],
  BASKAN: [
    { label: 'Tüm Kayıtlar' },
    { label: 'Onay Bekleyenler', view: 'onay-bekleyenler' },
    { label: 'Onaylananlar', view: 'onaylananlar' },
    { label: 'Reddedilenler', view: 'reddedilenler' },
  ],
  ADMIN: [],
}

export const recordSection = {
  label: 'Kayıtlar',
  icon: FolderKanban,
}

export const notificationNavigation = {
  label: 'Bildirimler',
  to: '/bildirimler',
  icon: Bell,
}
