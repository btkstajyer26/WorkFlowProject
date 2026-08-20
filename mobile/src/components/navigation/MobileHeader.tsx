import { useRouter } from 'expo-router';
import Moon from 'lucide-react-native/icons/moon';
import Sun from 'lucide-react-native/icons/sun';
import UserRound from 'lucide-react-native/icons/user-round';
import { Pressable, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { AppText } from '@/components/ui/AppText';
import { useAppTheme } from '@/theme/ThemeProvider';
import { appTokens } from '@/theme/theme';

export function MobileHeader() {
  const router = useRouter();
  const { colors, resolvedTheme, toggleTheme } = useAppTheme();
  const isDark = resolvedTheme === 'dark';

  return (
    <SafeAreaView
      className="border-b border-app-border bg-app-surface dark:border-app-border-dark dark:bg-app-surface-dark"
      edges={['top']}
    >
      <View className="h-16 flex-row items-center justify-between px-5">
        <Pressable
          accessibilityLabel="Profil ekranını aç"
          accessibilityRole="button"
          className="size-10 items-center justify-center rounded-app-pill bg-app-surface-strong active:bg-app-surface-selected dark:bg-app-surface-strong-dark dark:active:bg-app-surface-selected-dark"
          hitSlop={8}
          onPress={() => router.navigate('/(app)/profil')}
        >
          <UserRound color={colors.textMuted} size={21} strokeWidth={2} />
        </Pressable>

        <AppText accessibilityRole="header" variant="heading">
          EBYS
        </AppText>

        <Pressable
          accessibilityLabel={isDark ? 'Açık temaya geç' : 'Koyu temaya geç'}
          accessibilityRole="button"
          className="size-10 items-center justify-center rounded-app-md active:bg-app-surface-strong dark:active:bg-app-surface-strong-dark"
          hitSlop={8}
          onPress={() => void toggleTheme()}
        >
          {isDark ? (
            <Sun color={appTokens.brand[300]} size={21} strokeWidth={2} />
          ) : (
            <Moon color={appTokens.brand[700]} size={21} strokeWidth={2} />
          )}
        </Pressable>
      </View>
    </SafeAreaView>
  );
}
