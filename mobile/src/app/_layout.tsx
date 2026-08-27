import { Stack } from 'expo-router';
import { useFonts } from 'expo-font';
import * as SplashScreen from 'expo-splash-screen';
import { StatusBar } from 'expo-status-bar';
import { useEffect } from 'react';
import { View } from 'react-native';

import '../../global.css';
import { AuthProvider, useAuth } from '@/auth/AuthProvider';
import { OfflineBanner } from '@/components/feedback/OfflineBanner';
import { QueryProvider, useNetworkStatus } from '@/query/QueryProvider';
import { ThemeProvider, useAppTheme } from '@/theme/ThemeProvider';

void SplashScreen.preventAutoHideAsync();

export default function RootLayout() {
  const [fontsLoaded, fontError] = useFonts({
    Inter_400Regular: require('@expo-google-fonts/inter/400Regular/Inter_400Regular.ttf'),
    Inter_500Medium: require('@expo-google-fonts/inter/500Medium/Inter_500Medium.ttf'),
    Inter_600SemiBold: require('@expo-google-fonts/inter/600SemiBold/Inter_600SemiBold.ttf'),
    Inter_700Bold: require('@expo-google-fonts/inter/700Bold/Inter_700Bold.ttf'),
  });

  if (!fontsLoaded && !fontError) return null;

  return (
    <ThemeProvider>
      <QueryProvider>
        <AuthProvider>
          <RootNavigator />
        </AuthProvider>
      </QueryProvider>
    </ThemeProvider>
  );
}

function RootNavigator() {
  const {
    isAuthenticated,
    isReady: isAuthReady,
    mustChangePassword,
  } = useAuth();
  const { colors, isReady, resolvedTheme } = useAppTheme();
  const isOffline = useNetworkStatus();

  useEffect(() => {
    if (isReady && isAuthReady) void SplashScreen.hideAsync();
  }, [isAuthReady, isReady]);

  if (!isReady || !isAuthReady) return null;

  return (
    <View className="flex-1" style={{ backgroundColor: colors.canvas }}>
      <StatusBar style={resolvedTheme === 'dark' ? 'light' : 'dark'} />
      <OfflineBanner isOffline={isOffline} />
      <Stack
        screenOptions={{
          animation: 'fade',
          contentStyle: { backgroundColor: colors.canvas },
          headerShown: false,
        }}
      >
        <Stack.Protected guard={!isAuthenticated}>
          <Stack.Screen name="(auth)" />
        </Stack.Protected>
        <Stack.Protected guard={isAuthenticated && !mustChangePassword}>
          <Stack.Screen name="(app)" />
        </Stack.Protected>
        <Stack.Protected guard={isAuthenticated}>
          <Stack.Screen name="(password)" />
        </Stack.Protected>
      </Stack>
    </View>
  );
}
