import type { LucideIcon } from 'lucide-react-native';
import { Pressable, View } from 'react-native';

import { AppText } from '@/components/ui/AppText';
import { appTokens } from '@/theme/theme';

type SummaryTone = 'brand' | 'danger' | 'info' | 'success' | 'warning';

const toneClasses: Record<SummaryTone, string> = {
  brand: 'bg-brand-100 dark:bg-brand-900/40',
  danger: 'bg-rose-100 dark:bg-rose-950/50',
  info: 'bg-blue-100 dark:bg-blue-950/50',
  success: 'bg-emerald-100 dark:bg-emerald-950/50',
  warning: 'bg-amber-100 dark:bg-amber-950/50',
};

const iconColors: Record<SummaryTone, string> = {
  brand: appTokens.brand[600],
  danger: appTokens.feedback.danger,
  info: appTokens.feedback.info,
  success: appTokens.feedback.success,
  warning: appTokens.feedback.warning,
};

type DashboardSummaryCardProps = {
  icon: LucideIcon;
  isLoading: boolean;
  label: string;
  onPress: () => void;
  tone: SummaryTone;
  value: number;
};

export function DashboardSummaryCard({
  icon: Icon,
  isLoading,
  label,
  onPress,
  tone,
  value,
}: DashboardSummaryCardProps) {
  const displayedValue = isLoading ? '—' : String(value);

  return (
    <Pressable
      accessibilityLabel={`${label}: ${displayedValue}`}
      accessibilityRole="button"
      className="min-w-[46%] flex-1 rounded-app-lg border border-app-border bg-app-surface p-4 active:bg-app-surface-muted dark:border-app-border-dark dark:bg-app-surface-dark dark:active:bg-app-surface-muted-dark"
      onPress={onPress}
    >
      <View
        className={`size-10 items-center justify-center rounded-app-md ${toneClasses[tone]}`}
      >
        <Icon color={iconColors[tone]} size={20} strokeWidth={2.2} />
      </View>
      <AppText className="mt-4" numberOfLines={2} tone="muted" variant="caption">
        {label}
      </AppText>
      <AppText className="mt-1" variant="title">
        {displayedValue}
      </AppText>
    </Pressable>
  );
}

export type { SummaryTone };
