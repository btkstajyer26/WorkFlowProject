import type { ComponentProps, PropsWithChildren } from 'react';
import { View } from 'react-native';

type AppCardProps = PropsWithChildren<ComponentProps<typeof View>> & {
  className?: string;
};

export function AppCard({ children, className = '', ...props }: AppCardProps) {
  return (
    <View
      className={`rounded-app-lg border border-app-border bg-app-surface p-4 dark:border-app-border-dark dark:bg-app-surface-dark ${className}`}
      {...props}
    >
      {children}
    </View>
  );
}
