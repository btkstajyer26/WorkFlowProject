import { Pressable, View } from 'react-native';

import type { RecordListItem as RecordListItemModel } from '@/api/records';
import { AppText } from '@/components/ui/AppText';

import { RecordStatusBadge } from './RecordStatusBadge';

function formatRecordDate(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return 'Tarih bilgisi yok';

  return new Intl.DateTimeFormat('tr-TR', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  }).format(date);
}

type RecordListItemProps = {
  categoryName: string;
  onPress: () => void;
  record: RecordListItemModel;
};

export function RecordListItem({
  categoryName,
  onPress,
  record,
}: RecordListItemProps) {
  return (
    <Pressable
      accessibilityHint="Kayıt detayını açar"
      accessibilityLabel={`${record.title}, ${categoryName}`}
      accessibilityRole="button"
      className="gap-3 rounded-app-lg border border-app-border bg-app-surface p-4 active:bg-app-surface-muted dark:border-app-border-dark dark:bg-app-surface-dark dark:active:bg-app-surface-muted-dark"
      onPress={onPress}
    >
      <View className="flex-row items-start justify-between gap-3">
        <AppText className="min-w-0 flex-1" numberOfLines={2} variant="heading">
          {record.title}
        </AppText>
        <RecordStatusBadge status={record.status} />
      </View>

      <AppText numberOfLines={2} tone="muted">
        {record.description}
      </AppText>

      <View className="flex-row flex-wrap items-center gap-2">
        <View className="rounded-app-pill bg-brand-50 px-2.5 py-1 dark:bg-brand-900/30">
          <AppText tone="brand" variant="caption">
            {categoryName}
          </AppText>
        </View>
        <AppText tone="muted" variant="caption">
          {formatRecordDate(record.createdAt)}
        </AppText>
      </View>

      <AppText numberOfLines={1} tone="muted" variant="caption">
        Oluşturan: {record.createdByFullName?.trim() || record.createdBy}
      </AppText>
    </Pressable>
  );
}
