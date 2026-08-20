import { useRouter } from 'expo-router';
import ArrowRight from 'lucide-react-native/icons/arrow-right';
import { View } from 'react-native';

import { AppButton } from '@/components/ui/AppButton';
import { AppCard } from '@/components/ui/AppCard';
import { AppText } from '@/components/ui/AppText';
import { Screen } from '@/components/ui/Screen';
import { appTokens } from '@/theme/theme';

export default function LoginPlaceholderScreen() {
  const router = useRouter();

  return (
    <Screen>
      <View className="flex-1 justify-center px-6">
        <AppCard className="gap-3">
          <AppText accessibilityRole="header" variant="display">
            EBYS Mobil
          </AppText>
          <AppText tone="muted">
            Mobil uygulama altyapısı hazır. Giriş akışı sonraki aşamada eklenecek.
          </AppText>
          <AppButton
            icon={<ArrowRight color={appTokens.content.onBrand} size={18} />}
            label="Uygulama iskeletini önizle"
            onPress={() => router.replace('/(app)')}
          />
        </AppCard>
      </View>
    </Screen>
  );
}
