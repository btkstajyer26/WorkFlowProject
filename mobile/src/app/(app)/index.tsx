import { View } from 'react-native';

import { AppCard } from '@/components/ui/AppCard';
import { AppText } from '@/components/ui/AppText';
import { Screen } from '@/components/ui/Screen';

export default function AppPlaceholderScreen() {
  return (
    <Screen>
      <View className="flex-1 justify-center px-6">
        <AppCard className="gap-3">
          <AppText accessibilityRole="header" variant="title">
          Uygulama alanı
          </AppText>
          <AppText tone="muted">
            Yetkili ekranlar, oturum altyapısı tamamlandıktan sonra kendi özellik
            sahipleri tarafından eklenecek.
          </AppText>
        </AppCard>
      </View>
    </Screen>
  );
}
