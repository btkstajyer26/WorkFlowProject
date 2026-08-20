import { View } from 'react-native';

import { AppCard } from '@/components/ui/AppCard';
import { AppText } from '@/components/ui/AppText';
import { Screen } from '@/components/ui/Screen';

export default function LoginPlaceholderScreen() {
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
        </AppCard>
      </View>
    </Screen>
  );
}
