import { StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

export default function AppPlaceholderScreen() {
  return (
    <SafeAreaView style={styles.safeArea}>
      <View style={styles.content}>
        <Text accessibilityRole="header" style={styles.title}>
          Uygulama alanı
        </Text>
        <Text style={styles.description}>
          Yetkili ekranlar, oturum altyapısı tamamlandıktan sonra kendi özellik sahipleri tarafından eklenecek.
        </Text>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: '#f7f8fc',
  },
  content: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 24,
  },
  title: {
    color: '#0f172a',
    fontSize: 24,
    fontWeight: '700',
  },
  description: {
    marginTop: 12,
    maxWidth: 340,
    color: '#475569',
    fontSize: 15,
    lineHeight: 23,
    textAlign: 'center',
  },
});
