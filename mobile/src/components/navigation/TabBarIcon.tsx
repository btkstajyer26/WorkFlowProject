import type { LucideIcon } from "lucide-react-native";
import { type ColorValue, View } from "react-native";

import { appTokens } from "@/theme/theme";

type TabBarIconProps = {
  color: ColorValue;
  focused: boolean;
  icon: LucideIcon;
  prominent?: boolean;
};

export function TabBarIcon({
  color,
  focused,
  icon: Icon,
  prominent = false,
}: TabBarIconProps) {
  const backgroundClass = prominent
    ? focused
      ? "bg-brand-700"
      : "bg-brand-600"
    : focused
      ? "bg-brand-100 dark:bg-brand-900/40"
      : "bg-transparent";

  return (
    <View
      className={`items-center justify-center rounded-app-md ${
        prominent
          ? "size-10 rounded-full shadow-lg"
          : "min-w-10 rounded-app-md px-2 py-1"
      } ${backgroundClass}`}
      style={
        prominent
          ? {
              elevation: 8,
              shadowColor: "#000000",
              shadowOffset: { height: 4, width: 0 },
              shadowOpacity: 0.25,
              shadowRadius: 6,
            }
          : undefined
      }
    >
      <Icon
        color={prominent ? appTokens.content.onBrand : (color as string)}
        size={prominent ? 24 : 22}
        strokeWidth={prominent ? 2.7 : focused ? 2.4 : 2}
      />
    </View>
  );
}
