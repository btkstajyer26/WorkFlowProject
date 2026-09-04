/**
 * Yerleşik rollerin **değişmez** teknik anahtarları (`roles.system_key`).
 * Panelden açılan dinamik rollerde bu alan `null`'dur.
 */
export type SystemRoleKey = 'CALISAN' | 'BASKAN_YARDIMCISI' | 'BASKAN' | 'ADMIN'

export type WorkflowRole = 'CALISAN' | 'BASKAN_YARDIMCISI' | 'BASKAN'
export type UserRole = WorkflowRole | 'ADMIN'

/**
 * Oturum kullanıcısı.
 *
 * Rol üç alanla taşınır ve karıştırılmamalıdır:
 *
 * - `roleId` — ilişkisel kimlik.
 * - `systemKey` — yerleşik rolün değişmez anahtarı; **davranış kararları bunun
 *   üzerinden verilir**. Dinamik rolde `null`'dur ve o kullanıcı hiçbir sistem
 *   rolüne özel arayüz almaz.
 * - `roleName` — yalnızca **gösterim adı**. AP-2 ile panelden değiştirilebilir;
 *   sabit bir listeye karşı doğrulanmaz ve koşullarda kullanılmaz.
 */
export type AuthUser = {
  id: string
  firstName: string
  lastName: string
  email: string
  roleId: number
  systemKey: SystemRoleKey | null
  roleName: string
  mustChangePassword: boolean
}

const systemRoleKeys: SystemRoleKey[] = ['CALISAN', 'BASKAN_YARDIMCISI', 'BASKAN', 'ADMIN']

/**
 * Sunucudan gelen `systemKey`'i bilinen yerleşik anahtarlara daraltır.
 * Tanınmayan veya boş değer dinamik rol demektir — hata değildir.
 */
export function toSystemRoleKey(value: string | null | undefined): SystemRoleKey | null {
  return value && systemRoleKeys.includes(value as SystemRoleKey) ? (value as SystemRoleKey) : null
}

/**
 * Yerleşik roller için yedek etiketler. Sunucu gösterim adını gönderdiği için
 * normalde `roleName` kullanılır; bu tablo yalnızca ad gelmediğinde devreye girer.
 */
export const roleLabels: Record<SystemRoleKey, string> = {
  CALISAN: 'Çalışan',
  BASKAN_YARDIMCISI: 'Başkan Yardımcısı',
  BASKAN: 'Başkan',
  ADMIN: 'Sistem Yöneticisi',
}

/**
 * Kullanıcıya gösterilecek rol etiketi.
 *
 * Yerleşik roller veritabanında teknik adlarıyla (`roles.name = 'CALISAN'`)
 * gelir; kullanıcıya "Çalışan" gösterilmelidir. Admin bu rolü yeniden
 * adlandırdıysa (`roleName !== systemKey`) artık onun seçtiği ad kazanır.
 * Dinamik rollerde tek kaynak zaten sunucudan gelen addır.
 */
export function roleLabelOf(user: { roleName?: string | null; systemKey: SystemRoleKey | null }): string {
  const name = user.roleName?.trim()
  if (user.systemKey && (!name || name === user.systemKey)) return roleLabels[user.systemKey]
  if (name) return name
  return 'Tanımsız rol'
}
