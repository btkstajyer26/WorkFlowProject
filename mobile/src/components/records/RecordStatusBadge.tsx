import { View } from 'react-native';

import type { RecordStatus } from '@/api/records';
import { AppText } from '@/components/ui/AppText';

type StatusMeta = {
  containerClassName: string;
  label: string;
  textClassName: string;
};

export const recordStatusMeta: Record<RecordStatus, StatusMeta> = {
  BASKAN_INCELEMESINDE: {
    containerClassName: 'bg-blue-100 dark:bg-blue-950/50',
    label: 'Başkan incelemesinde',
    textClassName: 'text-blue-700 dark:text-blue-300',
  },
  BSK_YRD_INCELEMESINDE: {
    containerClassName: 'bg-amber-100 dark:bg-amber-950/50',
    label: 'Bşk. Yrd. incelemesinde',
    textClassName: 'text-amber-700 dark:text-amber-300',
  },
  DUZENLEME_BEKLIYOR: {
    containerClassName: 'bg-orange-100 dark:bg-orange-950/50',
    label: 'Düzenleme bekliyor',
    textClassName: 'text-orange-700 dark:text-orange-300',
  },
  ONAYLANDI: {
    containerClassName: 'bg-emerald-100 dark:bg-emerald-950/50',
    label: 'Onaylandı',
    textClassName: 'text-emerald-700 dark:text-emerald-300',
  },
  REDDEDILDI: {
    containerClassName: 'bg-rose-100 dark:bg-rose-950/50',
    label: 'Reddedildi',
    textClassName: 'text-rose-700 dark:text-rose-300',
  },
  TASLAK: {
    containerClassName:
      'bg-app-surface-strong dark:bg-app-surface-strong-dark',
    label: 'Taslak',
    textClassName: 'text-app-text-muted dark:text-app-text-muted-dark',
  },
};

export function RecordStatusBadge({ status }: { status: RecordStatus }) {
  const meta = recordStatusMeta[status];

  return (
    <View
      accessibilityLabel={`Durum: ${meta.label}`}
      className={`self-start rounded-app-pill px-2.5 py-1 ${meta.containerClassName}`}
    >
      <AppText className={meta.textClassName} variant="caption">
        {meta.label}
      </AppText>
    </View>
  );
}
