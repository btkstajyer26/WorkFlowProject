import { Redirect } from 'expo-router';

import { useAuth } from '@/auth/AuthProvider';

export default function IndexScreen() {
  const { isAuthenticated, mustChangePassword } = useAuth();

  if (!isAuthenticated) return <Redirect href="/(auth)/giris" />;
  if (mustChangePassword) return <Redirect href="/(password)/sifre-degistir" />;

  return <Redirect href="/(app)" />;
}
