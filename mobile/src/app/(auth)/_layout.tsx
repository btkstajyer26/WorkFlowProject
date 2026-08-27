import { Redirect, Stack } from 'expo-router';

import { useAuth } from '@/auth/AuthProvider';

export default function AuthLayout() {
  const { isAuthenticated, mustChangePassword } = useAuth();

  if (isAuthenticated) {
    return (
      <Redirect
        href={mustChangePassword ? '/(password)/sifre-degistir' : '/(app)'}
      />
    );
  }

  return <Stack screenOptions={{ headerShown: false }} />;
}
