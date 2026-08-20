import type { ComponentProps } from 'react';
import { Text } from 'react-native';

type TextVariant = 'display' | 'title' | 'heading' | 'body' | 'label' | 'caption';
type TextTone =
  | 'default'
  | 'strong'
  | 'muted'
  | 'brand'
  | 'danger'
  | 'inverse';

const variantClasses: Record<TextVariant, string> = {
  display: 'font-inter-bold text-[28px] leading-[34px]',
  title: 'font-inter-bold text-2xl leading-[30px]',
  heading: 'font-inter-semibold text-lg leading-6',
  body: 'font-inter text-[15px] leading-[23px]',
  label: 'font-inter-semibold text-sm leading-5',
  caption: 'font-inter-medium text-xs leading-4',
};

const toneClasses: Record<TextTone, string> = {
  default: 'text-app-text dark:text-app-text-dark',
  strong: 'text-app-text-strong dark:text-app-text-strong-dark',
  muted: 'text-app-text-muted dark:text-app-text-muted-dark',
  brand: 'text-brand-700 dark:text-brand-300',
  danger: 'text-rose-600 dark:text-rose-400',
  inverse: 'text-white dark:text-white',
};

type AppTextProps = ComponentProps<typeof Text> & {
  className?: string;
  tone?: TextTone;
  variant?: TextVariant;
};

export function AppText({
  className = '',
  tone = 'default',
  variant = 'body',
  ...props
}: AppTextProps) {
  return (
    <Text
      className={`${variantClasses[variant]} ${toneClasses[tone]} ${className}`}
      {...props}
    />
  );
}
