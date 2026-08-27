import { WifiOff } from 'lucide-react-native';
import { View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { AppText } from '@/components/ui/AppText';

interface OfflineBannerProps {
  isOffline: boolean;
}

export function OfflineBanner({ isOffline }: OfflineBannerProps) {
  if (!isOffline) return null;

  return (
    <SafeAreaView className="bg-amber-400" edges={['top']}>
      <View
        accessibilityLabel="İnternet bağlantısı yok"
        accessibilityLiveRegion="polite"
        accessibilityRole="alert"
        className="flex-row items-center justify-center gap-2 px-4 py-2"
      >
        <WifiOff aria-hidden color="#451a03" size={16} strokeWidth={2.25} />
        <AppText className="text-center text-amber-950" variant="caption">
          İnternet bağlantısı yok. Bağlantı gelince veriler yenilenecek.
        </AppText>
      </View>
    </SafeAreaView>
  );
}
