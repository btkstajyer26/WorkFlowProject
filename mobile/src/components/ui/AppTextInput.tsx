import type { ComponentProps } from 'react';
import { TextInput, View } from 'react-native';

import { useAppTheme } from '@/theme/ThemeProvider';

import { AppText } from './AppText';

type AppTextInputProps = ComponentProps<typeof TextInput> & {
  error?: string;
  label: string;
};

export function AppTextInput({
  accessibilityHint,
  className = '',
  error,
  label,
  ...props
}: AppTextInputProps) {
  const { colors } = useAppTheme();

  return (
    <View className="gap-1.5">
      <AppText variant="label">{label}</AppText>
      <TextInput
        accessibilityHint={error ?? accessibilityHint}
        accessibilityLabel={label}
        className={`min-h-12 rounded-app-md border bg-app-surface-strong px-4 font-inter text-[15px] text-app-text-strong dark:bg-app-surface-strong-dark dark:text-app-text-strong-dark ${
          error
            ? 'border-rose-500 dark:border-rose-400'
            : 'border-app-border dark:border-app-border-dark'
        } ${className}`}
        placeholderTextColor={colors.textFaint}
        selectionColor={colors.textMuted}
        {...props}
      />
      {error ? (
        <AppText accessibilityLiveRegion="polite" tone="danger" variant="caption">
          {error}
        </AppText>
      ) : null}
    </View>
  );
}
