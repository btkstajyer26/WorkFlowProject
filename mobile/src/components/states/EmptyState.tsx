import React from 'react';
import { View, Text } from 'react-native';

interface EmptyStateProps {
  title?: string;
  message?: string;
}

export const EmptyState: React.FC<EmptyStateProps> = ({
  title = 'Kayıt Bulunamadı',
  message = 'Görüntülenecek herhangi bir içerik bulunmuyor.',
}) => {
  return (
    <View className="flex-1 items-center justify-center p-6">
      <Text className="text-base font-semibold text-gray-700 mb-1 text-center">{title}</Text>
      <Text className="text-xs text-gray-400 text-center">{message}</Text>
    </View>
  );
};