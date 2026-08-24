import React from 'react';
import { View, Text, TouchableOpacity } from 'react-native';

interface ErrorStateProps {
  title?: string;
  message?: string;
  onRetry?: () => void;
}

export const ErrorState: React.FC<ErrorStateProps> = ({
  title = 'Bir hata oluştu',
  message = 'Veriler alınırken beklenmedik bir sorun yaşandı.',
  onRetry,
}) => {
  return (
    <View className="flex-1 items-center justify-center p-6">
      <Text className="text-lg font-bold text-red-600 mb-2 text-center">{title}</Text>
      <Text className="text-sm text-gray-600 text-center mb-6">{message}</Text>
      {onRetry && (
        <TouchableOpacity
          onPress={onRetry}
          activeOpacity={0.8}
          className="bg-blue-600 px-5 py-2.5 rounded-lg active:bg-blue-700"
          accessibilityRole="button"
          accessibilityLabel="Tekrar dene"
        >
          <Text className="text-white font-semibold text-sm">Tekrar Dene</Text>
        </TouchableOpacity>
      )}
    </View>
  );
};