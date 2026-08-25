import { View } from 'react-native';

import type { AuditLog } from '@/api/auditLogs';
import { AppCard } from '@/components/ui/AppCard';
import { AppText } from '@/components/ui/AppText';

const auditActionLabels: Record<string, string> = {
  BASKANA_ILET: 'Başkana iletildi',
  BASKAN_YARDIMCISINA_GERI_GONDER: 'Başkan yardımcısına geri gönderildi',
  CALISANA_GERI_GONDER: 'Çalışana geri gönderildi',
  GONDER: 'İncelemeye gönderildi',
  ONAYLA: 'Onaylandı',
  REDDET: 'Reddedildi',
  TEKRAR_GONDER: 'Tekrar incelemeye gönderildi',
};

function formatDate(value: string) {
  return new Intl.DateTimeFormat('tr-TR', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value));
}

export function RecordHistory({ logs }: { logs: AuditLog[] }) {
  return (
    <View className="gap-3">
      <View className="gap-1">
        <AppText accessibilityRole="header" variant="heading">
          İşlem geçmişi
        </AppText>
        <AppText tone="muted" variant="caption">
          Yetkinize göre görüntülemenize izin verilen adımlar gösterilir.
        </AppText>
      </View>

      {logs.length === 0 ? (
        <AppCard>
          <AppText tone="muted">
            Bu kayıt için görüntülenebilir bir işlem geçmişi bulunmuyor.
          </AppText>
        </AppCard>
      ) : (
        logs.map((log, index) => (
          <View className="flex-row gap-3" key={log.id}>
            <View className="items-center">
              <View className="mt-1.5 size-3 rounded-app-pill bg-brand-600" />
              {index < logs.length - 1 ? (
                <View className="w-px flex-1 bg-app-border dark:bg-app-border-dark" />
              ) : null}
            </View>
            <AppCard className="mb-3 min-w-0 flex-1 gap-2">
              <AppText variant="label">
                {auditActionLabels[log.action] ?? log.action}
              </AppText>
              <AppText tone="muted" variant="caption">
                {log.userFullName ?? 'Sistem'}
                {log.roleName ? ` · ${log.roleName}` : ''}
              </AppText>
              <AppText tone="muted" variant="caption">
                {formatDate(log.createdAt)}
              </AppText>
              {log.comment ? <AppText>{log.comment}</AppText> : null}
              {log.errorCode ? (
                <AppText tone="danger" variant="caption">
                  İşlem hatası: {log.errorCode}
                </AppText>
              ) : null}
            </AppCard>
          </View>
        ))
      )}
    </View>
  );
}
