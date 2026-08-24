import React from 'react';
import { View, ActivityIndicator, Text } from 'react-native';

interface LoadingStateProps {
  message?: string;
}

export const LoadingState: React.FC<LoadingStateProps> = ({ message = 'Yükleniyor...' }) => {
  return (
    <View className="flex-1 items-center justify-center p-6 bg-transparent">
      <ActivityIndicator size="large" color="#2563eb" />
      <Text className="mt-3 text-sm text-gray-500 font-medium">{message}</Text>
    </View>
  );
};