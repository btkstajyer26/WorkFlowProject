import type { ComponentProps, PropsWithChildren } from 'react';
import { SafeAreaView } from 'react-native-safe-area-context';

type ScreenProps = PropsWithChildren<ComponentProps<typeof SafeAreaView>> & {
  className?: string;
};

export function Screen({ children, className = '', ...props }: ScreenProps) {
  return (
    <SafeAreaView
      className={`flex-1 bg-app-canvas dark:bg-app-canvas-dark ${className}`}
      {...props}
    >
      {children}
    </SafeAreaView>
  );
}
