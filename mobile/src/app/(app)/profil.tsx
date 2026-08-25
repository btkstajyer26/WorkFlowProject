import { useRouter } from 'expo-router';
import KeyRound from 'lucide-react-native/icons/key-round';
import LogOut from 'lucide-react-native/icons/log-out';
import Mail from 'lucide-react-native/icons/mail';
import ShieldCheck from 'lucide-react-native/icons/shield-check';
import UserRound from 'lucide-react-native/icons/user-round';
import { ActivityIndicator, RefreshControl, ScrollView, View } from 'react-native';

import { useAuth } from '@/auth/AuthProvider';
import { AppButton } from '@/components/ui/AppButton';
import { AppCard } from '@/components/ui/AppCard';
import { AppText } from '@/components/ui/AppText';
import { Screen } from '@/components/ui/Screen';
import { userRoleLabels } from '@/constants/userRoles';
import { useCurrentUser } from '@/query/currentUser';
import { useAppTheme } from '@/theme/ThemeProvider';
import { appTokens } from '@/theme/theme';

export default function ProfileScreen() {
  const router = useRouter();
  const { signOut } = useAuth();
  const { colors } = useAppTheme();
  const currentUser = useCurrentUser();

  if (currentUser.isPending) {
    return (
      <Screen className="items-center justify-center" edges={['left', 'right']}>
        <ActivityIndicator color={appTokens.brand[600]} size="large" />
        <AppText className="mt-3" tone="muted">
          Profiliniz yükleniyor…
        </AppText>
      </Screen>
    );
  }

  if (currentUser.isError) {
    return (
      <Screen className="justify-center px-5" edges={['left', 'right']}>
        <AppCard className="gap-4 p-5">
          <View className="items-center gap-2">
            <AppText accessibilityRole="header" variant="heading">
              Profil yüklenemedi
            </AppText>
            <AppText className="text-center" tone="muted">
              Bağlantınızı kontrol edip yeniden deneyin.
            </AppText>
          </View>
          <AppButton label="Tekrar dene" onPress={() => void currentUser.refetch()} />
        </AppCard>
      </Screen>
    );
  }

  const user = currentUser.data;
  const fullName = `${user.firstName} ${user.lastName}`;
  const initials = `${user.firstName.charAt(0)}${user.lastName.charAt(0)}`.toLocaleUpperCase('tr-TR');

  return (
    <Screen edges={['left', 'right']}>
      <ScrollView
        contentContainerClassName="gap-4 px-5 py-5"
        refreshControl={
          <RefreshControl
            onRefresh={() => void currentUser.refetch()}
            refreshing={currentUser.isRefetching}
            tintColor={colors.textMuted}
          />
        }
      >
        <View className="gap-1">
          <AppText accessibilityRole="header" variant="title">
            Profil
          </AppText>
          <AppText tone="muted">Temel kullanıcı ve yetki bilgilerinizi görüntüleyin.</AppText>
        </View>

        <AppCard className="gap-5 p-5">
          <View className="flex-row items-center gap-4">
            <View
              accessibilityLabel={`${fullName} profil görseli`}
              className="size-16 items-center justify-center rounded-app-lg bg-brand-100 dark:bg-brand-900/40"
            >
              <AppText className="text-xl" tone="brand" variant="heading">
                {initials}
              </AppText>
            </View>
            <View className="min-w-0 flex-1 gap-1">
              <AppText numberOfLines={1} variant="heading">
                {fullName}
              </AppText>
              <View className="flex-row items-center gap-1.5">
                <ShieldCheck color={appTokens.brand[500]} size={16} />
                <AppText tone="muted" variant="caption">
                  {userRoleLabels[user.roleName]}
                </AppText>
              </View>
            </View>
          </View>

          <View className="gap-3">
            <ProfileField icon="user" label="Ad soyad" value={fullName} />
            <ProfileField icon="mail" label="E-posta adresi" value={user.email} />
          </View>
        </AppCard>

        <AppCard className="gap-4 p-5">
          <View className="gap-1">
            <AppText variant="heading">Yetki ve güvenlik</AppText>
            <AppText tone="muted">
              Rolünüz ve hesap bilgileriniz kurum yöneticisi tarafından yönetilir.
            </AppText>
          </View>
          <AppButton
            icon={<KeyRound color={appTokens.brand[600]} size={18} />}
            label="Şifreyi değiştir"
            onPress={() => router.push('/(password)/sifre-degistir')}
            variant="secondary"
          />
          <AppButton
            icon={<LogOut color={appTokens.brand[600]} size={18} />}
            label="Çıkış yap"
            onPress={() => void signOut()}
            variant="ghost"
          />
        </AppCard>
      </ScrollView>
    </Screen>
  );
}

type ProfileFieldProps = {
  icon: 'mail' | 'user';
  label: string;
  value: string;
};

function ProfileField({ icon, label, value }: ProfileFieldProps) {
  const Icon = icon === 'mail' ? Mail : UserRound;

  return (
    <View className="flex-row items-center gap-3 rounded-app-md bg-app-surface-strong p-4 dark:bg-app-surface-strong-dark">
      <View className="size-10 items-center justify-center rounded-app-md bg-app-surface dark:bg-app-surface-dark">
        <Icon color={appTokens.brand[500]} size={18} />
      </View>
      <View className="min-w-0 flex-1">
        <AppText tone="muted" variant="caption">
          {label}
        </AppText>
        <AppText className="mt-0.5" numberOfLines={1} variant="label">
          {value}
        </AppText>
      </View>
    </View>
  );
}
