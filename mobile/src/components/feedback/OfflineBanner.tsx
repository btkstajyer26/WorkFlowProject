import React from 'react';
import { View, Text } from 'react-native';

interface OfflineBannerProps {
  isOffline: boolean;
}

export const OfflineBanner: React.FC<OfflineBannerProps> = ({ isOffline }) => {
  if (!isOffline) return null;

  return (
    <View className="bg-amber-500 px-4 py-2 flex-row items-center justify-center">
      <Text className="text-white text-xs font-medium text-center">
        İnternet bağlantısı yok. Bazı işlemler kısıtlanmış olabilir.
      </Text>
    </View>
  );
};