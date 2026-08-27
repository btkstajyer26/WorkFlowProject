import { Tabs } from "expo-router";
import Bell from "lucide-react-native/icons/bell";
import CirclePlus from "lucide-react-native/icons/circle-plus";
import Files from "lucide-react-native/icons/files";
import LayoutDashboard from "lucide-react-native/icons/layout-dashboard";
import UserRound from "lucide-react-native/icons/user-round";
import { StyleSheet } from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";

import { MobileHeader } from "@/components/navigation/MobileHeader";
import { TabBarIcon } from "@/components/navigation/TabBarIcon";
import { useCurrentUser } from "@/query/currentUser";
import { useUnreadNotificationCount } from "@/query/notifications";
import { useAppTheme } from "@/theme/ThemeProvider";
import { appTokens } from "@/theme/theme";

export default function AppLayout() {
  const insets = useSafeAreaInsets();
  const { colors, resolvedTheme } = useAppTheme();
  const currentUser = useCurrentUser();
  const unreadCountQuery = useUnreadNotificationCount();
  const unreadCount = unreadCountQuery.data ?? 0;
  const canCreateRecord = currentUser.data?.roleName === "CALISAN";
  const activeTintColor =
    resolvedTheme === "dark" ? appTokens.brand[300] : appTokens.brand[600];

  return (
    <Tabs
      backBehavior="history"
      screenOptions={{
        animation: "fade",
        header: () => <MobileHeader />,
        headerShown: true,
        sceneStyle: { backgroundColor: colors.canvas },
        tabBarActiveTintColor: activeTintColor,
        tabBarHideOnKeyboard: true,
        tabBarInactiveTintColor: colors.textSubtle,
        tabBarLabelStyle: styles.tabBarLabel,
        tabBarStyle: {
          backgroundColor: colors.surface,
          borderTopColor: colors.border,
          borderTopWidth: StyleSheet.hairlineWidth,
          height: 64 + insets.bottom,
          paddingBottom: Math.max(insets.bottom, 8),
          paddingTop: 7,
        },
      }}
    >
      <Tabs.Screen
        name="index"
        options={{
          tabBarAccessibilityLabel: "Panel sekmesi",
          tabBarIcon: ({ color, focused }) => (
            <TabBarIcon
              color={color}
              focused={focused}
              icon={LayoutDashboard}
            />
          ),
          title: "Dashboard",
        }}
      />
      <Tabs.Screen
        name="kayitlar"
        options={{
          tabBarAccessibilityLabel: "Kayıtlar sekmesi",
          tabBarIcon: ({ color, focused }) => (
            <TabBarIcon color={color} focused={focused} icon={Files} />
          ),
          title: "Kayıtlar",
        }}
      />
      <Tabs.Screen
        name="olustur"
        options={{
          href: canCreateRecord ? "/olustur" : null,
          tabBarAccessibilityLabel: "Yeni kayıt oluştur sekmesi",
          tabBarIcon: ({ color, focused }) => (
            <TabBarIcon
              color={color}
              focused={focused}
              icon={CirclePlus}
              prominent
            />
          ),
          tabBarIconStyle: {
            marginBottom: 4,
          },
          tabBarLabelPosition: "below-icon",
          title: "Oluştur",
        }}
      />
      <Tabs.Screen
        name="bildirimler"
        options={{
          tabBarAccessibilityLabel: "Bildirimler sekmesi",
          tabBarBadge:
            unreadCount > 0
              ? unreadCount > 99
                ? "99+"
                : unreadCount
              : undefined,
          tabBarBadgeStyle: styles.notificationBadge,
          tabBarIcon: ({ color, focused }) => (
            <TabBarIcon color={color} focused={focused} icon={Bell} />
          ),
          title: "Bildirimler",
        }}
      />
      <Tabs.Screen
        name="profil"
        options={{
          tabBarAccessibilityLabel: "Profil sekmesi",
          tabBarIcon: ({ color, focused }) => (
            <TabBarIcon color={color} focused={focused} icon={UserRound} />
          ),
          title: "Profil",
        }}
      />
    </Tabs>
  );
}

const styles = StyleSheet.create({
  notificationBadge: {
    alignItems: "center",
    backgroundColor: appTokens.brand[600],
    borderRadius: 8,
    color: "#ffffff",
    fontFamily: "Inter_700Bold",
    fontSize: 9,
    fontWeight: "700",
    height: 16,
    includeFontPadding: false,
    justifyContent: "center",
    lineHeight: 11,
    minWidth: 16,
    paddingHorizontal: 3,
    paddingVertical: 0,
    textAlign: "center",
    textAlignVertical: "center",
    top: 2,
  },
  tabBarLabel: {
    fontFamily: "Inter_600SemiBold",
    fontSize: 11,
    lineHeight: 14,
  },
});
