import type { LucideIcon } from 'lucide-react-native';
import { View } from 'react-native';

import { AppText } from '@/components/ui/AppText';
import { Screen } from '@/components/ui/Screen';
import { useAppTheme } from '@/theme/ThemeProvider';
import { appTokens } from '@/theme/theme';

type TabPlaceholderProps = {
  description: string;
  icon: LucideIcon;
  title: string;
};

export function TabPlaceholder({
  description,
  icon: Icon,
  title,
}: TabPlaceholderProps) {
  const { resolvedTheme } = useAppTheme();
  const iconColor =
    resolvedTheme === 'dark' ? appTokens.brand[300] : appTokens.brand[600];

  return (
    <Screen edges={['left', 'right']}>
      <View className="flex-1 items-center justify-center px-8 pb-8">
        <View className="mb-5 size-14 items-center justify-center rounded-app-lg bg-brand-100 dark:bg-brand-900/40">
          <Icon color={iconColor} size={26} strokeWidth={2} />
        </View>
        <AppText accessibilityRole="header" className="text-center" variant="title">
          {title}
        </AppText>
        <AppText className="mt-2 text-center" tone="muted">
          {description}
        </AppText>
      </View>
    </Screen>
  );
}
