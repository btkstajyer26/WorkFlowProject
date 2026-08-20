import type { ComponentProps, ReactNode } from 'react';
import { ActivityIndicator, Pressable, View } from 'react-native';

import { AppText } from './AppText';
import { appTokens } from '@/theme/theme';

type ButtonVariant = 'primary' | 'secondary' | 'ghost';

const containerClasses: Record<ButtonVariant, string> = {
  primary: 'bg-brand-700 active:bg-brand-800',
  secondary:
    'border border-app-border bg-app-surface active:bg-app-surface-strong dark:border-app-border-dark dark:bg-app-surface-dark dark:active:bg-app-surface-strong-dark',
  ghost: 'bg-transparent active:bg-brand-50 dark:active:bg-brand-900/30',
};

const labelTones: Record<ButtonVariant, 'brand' | 'inverse' | 'strong'> = {
  primary: 'inverse',
  secondary: 'strong',
  ghost: 'brand',
};

type AppButtonProps = Omit<ComponentProps<typeof Pressable>, 'children'> & {
  icon?: ReactNode;
  isLoading?: boolean;
  label: string;
  variant?: ButtonVariant;
};

export function AppButton({
  disabled,
  icon,
  isLoading = false,
  label,
  variant = 'primary',
  ...props
}: AppButtonProps) {
  const isDisabled = disabled || isLoading;

  return (
    <Pressable
      accessibilityRole="button"
      accessibilityState={{ busy: isLoading, disabled: isDisabled }}
      className={`min-h-12 flex-row items-center justify-center gap-2 rounded-app-md px-5 ${containerClasses[variant]} ${isDisabled ? 'opacity-50' : ''}`}
      disabled={isDisabled}
      {...props}
    >
      {isLoading ? (
        <ActivityIndicator
          color={
            variant === 'primary'
              ? appTokens.content.onBrand
              : appTokens.brand[600]
          }
        />
      ) : (
        <View className="flex-row items-center gap-2">
          {icon}
          <AppText tone={labelTones[variant]} variant="label">
            {label}
          </AppText>
        </View>
      )}
    </Pressable>
  );
}
