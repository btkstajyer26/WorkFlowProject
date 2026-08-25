import type { UserRole } from '@/api/users';

export const userRoleLabels: Record<UserRole, string> = {
  ADMIN: 'Admin',
  BASKAN: 'Başkan',
  BASKAN_YARDIMCISI: 'Başkan Yardımcısı',
  CALISAN: 'Çalışan',
};
