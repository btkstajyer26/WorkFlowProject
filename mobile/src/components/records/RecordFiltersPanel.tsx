import { useState } from 'react';
import { Pressable, ScrollView, View } from 'react-native';

import type { Category } from '@/api/categories';
import type { RecordStatus } from '@/api/records';
import { AppButton } from '@/components/ui/AppButton';
import { AppCard } from '@/components/ui/AppCard';
import { AppText } from '@/components/ui/AppText';
import { AppTextInput } from '@/components/ui/AppTextInput';

import { recordStatusMeta } from './RecordStatusBadge';

export type RecordListFilterValues = {
  categoryId?: number;
  fromDate: string;
  q: string;
  sort: 'createdAt,asc' | 'createdAt,desc';
  status?: RecordStatus;
  toDate: string;
};

export const defaultRecordListFilters: RecordListFilterValues = {
  fromDate: '',
  q: '',
  sort: 'createdAt,desc',
  toDate: '',
};

const statuses = Object.keys(recordStatusMeta) as RecordStatus[];

function parseDate(value: string, endOfDay: boolean): string | null | undefined {
  const normalizedValue = value.trim();
  if (!normalizedValue) return undefined;

  const match = /^(\d{2})\.(\d{2})\.(\d{4})$/.exec(normalizedValue);
  if (!match) return null;

  const [, dayText, monthText, yearText] = match;
  const day = Number(dayText);
  const month = Number(monthText);
  const year = Number(yearText);
  const date = new Date(year, month - 1, day);

  if (
    date.getFullYear() !== year ||
    date.getMonth() !== month - 1 ||
    date.getDate() !== day
  ) {
    return null;
  }

  return `${yearText}-${monthText}-${dayText}T${
    endOfDay ? '23:59:59' : '00:00:00'
  }`;
}

export function toApiDateRange(filters: RecordListFilterValues) {
  return {
    from: parseDate(filters.fromDate, false) ?? undefined,
    to: parseDate(filters.toDate, true) ?? undefined,
  };
}

type RecordFiltersPanelProps = {
  categories: Category[];
  initialValues: RecordListFilterValues;
  onApply: (filters: RecordListFilterValues) => void;
  onClear: () => void;
};

export function RecordFiltersPanel({
  categories,
  initialValues,
  onApply,
  onClear,
}: RecordFiltersPanelProps) {
  const [draft, setDraft] = useState(initialValues);
  const [dateError, setDateError] = useState<string>();

  const applyFilters = () => {
    const from = parseDate(draft.fromDate, false);
    const to = parseDate(draft.toDate, true);

    if (from === null || to === null) {
      setDateError('Tarihleri GG.AA.YYYY biçiminde yazın.');
      return;
    }

    if (from && to && from > to) {
      setDateError('Başlangıç tarihi bitiş tarihinden sonra olamaz.');
      return;
    }

    setDateError(undefined);
    onApply({
      ...draft,
      fromDate: draft.fromDate.trim(),
      q: draft.q.trim(),
      toDate: draft.toDate.trim(),
    });
  };

  const clearFilters = () => {
    setDateError(undefined);
    setDraft(defaultRecordListFilters);
    onClear();
  };

  return (
    <AppCard className="gap-5 p-4">
      <AppText variant="heading">Filtreler</AppText>

      <AppTextInput
        autoCapitalize="sentences"
        label="Başlık veya açıklama"
        onChangeText={(q) => setDraft((current) => ({ ...current, q }))}
        placeholder="Arama ifadesi"
        returnKeyType="search"
        value={draft.q}
      />

      <View className="gap-2">
        <AppText variant="label">Durum</AppText>
        <View className="flex-row flex-wrap gap-2">
          <FilterChip
            label="Tümü"
            onPress={() =>
              setDraft((current) => ({ ...current, status: undefined }))
            }
            selected={!draft.status}
          />
          {statuses.map((status) => (
            <FilterChip
              key={status}
              label={recordStatusMeta[status].label}
              onPress={() => setDraft((current) => ({ ...current, status }))}
              selected={draft.status === status}
            />
          ))}
        </View>
      </View>

      <View className="gap-2">
        <AppText variant="label">Kategori</AppText>
        <ScrollView
          contentContainerClassName="gap-2"
          horizontal
          showsHorizontalScrollIndicator={false}
        >
          <FilterChip
            label="Tümü"
            onPress={() =>
              setDraft((current) => ({ ...current, categoryId: undefined }))
            }
            selected={!draft.categoryId}
          />
          {categories.map((category) => (
            <FilterChip
              key={category.id}
              label={category.name}
              onPress={() =>
                setDraft((current) => ({ ...current, categoryId: category.id }))
              }
              selected={draft.categoryId === category.id}
            />
          ))}
        </ScrollView>
      </View>

      <View className="flex-row gap-3">
        <View className="flex-1">
          <AppTextInput
            error={dateError}
            label="Başlangıç"
            maxLength={10}
            onChangeText={(fromDate) =>
              setDraft((current) => ({ ...current, fromDate }))
            }
            placeholder="GG.AA.YYYY"
            value={draft.fromDate}
          />
        </View>
        <View className="flex-1">
          <AppTextInput
            label="Bitiş"
            maxLength={10}
            onChangeText={(toDate) =>
              setDraft((current) => ({ ...current, toDate }))
            }
            placeholder="GG.AA.YYYY"
            value={draft.toDate}
          />
        </View>
      </View>

      <View className="gap-2">
        <AppText variant="label">Sıralama</AppText>
        <View className="flex-row gap-2">
          <FilterChip
            label="En yeni"
            onPress={() =>
              setDraft((current) => ({ ...current, sort: 'createdAt,desc' }))
            }
            selected={draft.sort === 'createdAt,desc'}
          />
          <FilterChip
            label="En eski"
            onPress={() =>
              setDraft((current) => ({ ...current, sort: 'createdAt,asc' }))
            }
            selected={draft.sort === 'createdAt,asc'}
          />
        </View>
      </View>

      <View className="flex-row gap-3">
        <View className="flex-1">
          <AppButton label="Temizle" onPress={clearFilters} variant="secondary" />
        </View>
        <View className="flex-1">
          <AppButton label="Uygula" onPress={applyFilters} />
        </View>
      </View>
    </AppCard>
  );
}

type FilterChipProps = {
  label: string;
  onPress: () => void;
  selected: boolean;
};

function FilterChip({ label, onPress, selected }: FilterChipProps) {
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityState={{ selected }}
      className={`min-h-10 justify-center rounded-app-pill border px-3.5 py-2 ${
        selected
          ? 'border-brand-600 bg-brand-100 dark:border-brand-400 dark:bg-brand-900/40'
          : 'border-app-border bg-app-surface dark:border-app-border-dark dark:bg-app-surface-dark'
      }`}
      onPress={onPress}
    >
      <AppText tone={selected ? 'brand' : 'muted'} variant="caption">
        {label}
      </AppText>
    </Pressable>
  );
}
