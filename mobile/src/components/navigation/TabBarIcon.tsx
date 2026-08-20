import type { LucideIcon } from 'lucide-react-native';
import { type ColorValue, View } from 'react-native';

type TabBarIconProps = {
  color: ColorValue;
  focused: boolean;
  icon: LucideIcon;
};

export function TabBarIcon({ color, focused, icon: Icon }: TabBarIconProps) {
  return (
    <View
      className={`min-w-10 items-center justify-center rounded-app-md px-2 py-1 ${
        focused ? 'bg-brand-100 dark:bg-brand-900/40' : 'bg-transparent'
      }`}
    >
      <Icon color={color as string} size={22} strokeWidth={focused ? 2.4 : 2} />
    </View>
  );
}
