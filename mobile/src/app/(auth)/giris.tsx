import { zodResolver } from '@hookform/resolvers/zod';
import { Image } from 'expo-image';
import { useLocalSearchParams, useRouter } from 'expo-router';
import KeyRound from 'lucide-react-native/icons/key-round';
import LogIn from 'lucide-react-native/icons/log-in';
import { useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { KeyboardAvoidingView, Platform, ScrollView, View } from 'react-native';
import { z } from 'zod';

import { ApiClientError } from '@/api/errors';
import { useAuth } from '@/auth/AuthProvider';
import { AppButton } from '@/components/ui/AppButton';
import { AppCard } from '@/components/ui/AppCard';
import { AppText } from '@/components/ui/AppText';
import { AppTextInput } from '@/components/ui/AppTextInput';
import { Screen } from '@/components/ui/Screen';
import { appTokens } from '@/theme/theme';

const loginSchema = z.object({
  email: z.string().trim().min(1, 'E-posta adresinizi yazın.').email('Geçerli bir e-posta yazın.'),
  password: z.string().min(1, 'Şifrenizi yazın.'),
});

type LoginFormValues = z.infer<typeof loginSchema>;

export default function LoginScreen() {
  const router = useRouter();
  const { reason } = useLocalSearchParams<{ reason?: string }>();
  const { signIn } = useAuth();
  const [submitError, setSubmitError] = useState<string | null>(null);
  const {
    control,
    formState: { isSubmitting },
    handleSubmit,
    setFocus,
  } = useForm<LoginFormValues>({
    defaultValues: { email: '', password: '' },
    resolver: zodResolver(loginSchema),
  });

  const submitLogin = handleSubmit(async (values) => {
    setSubmitError(null);

    try {
      await signIn(values);
    } catch (error) {
      setSubmitError(
        error instanceof ApiClientError
          ? error.message
          : 'Giriş yapılamadı. Lütfen tekrar deneyin.',
      );
    }
  });

  return (
    <Screen>
      <KeyboardAvoidingView
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
        className="flex-1"
      >
        <ScrollView
          contentContainerStyle={{ flexGrow: 1, justifyContent: 'center' }}
          keyboardShouldPersistTaps="handled"
        >
          <View className="px-6 py-8">
            <AppCard className="gap-5 p-6">
              <View className="items-center gap-3">
                <Image
                  accessibilityIgnoresInvertColors
                  accessibilityLabel="EBYS logosu"
                  contentFit="contain"
                  source={require('../../../assets/images/ebys-logo.png')}
                  style={{ height: 68, width: 68 }}
                />
                <View className="items-center gap-1">
                  <AppText accessibilityRole="header" variant="display">
                    Hoş geldiniz
                  </AppText>
                  <AppText className="text-center" tone="muted">
                    Hesabınıza giriş yaparak kayıt süreçlerinizi yönetin.
                  </AppText>
                </View>
              </View>

              <Controller
                control={control}
                name="email"
                render={({ field: { onBlur, onChange, value }, fieldState: { error } }) => (
                  <AppTextInput
                    autoCapitalize="none"
                    autoComplete="email"
                    error={error?.message}
                    inputMode="email"
                    label="E-posta adresi"
                    onBlur={onBlur}
                    onChangeText={onChange}
                    onSubmitEditing={() => setFocus('password')}
                    placeholder="ornek@kurum.gov.tr"
                    returnKeyType="next"
                    textContentType="username"
                    value={value}
                  />
                )}
              />

              <Controller
                control={control}
                name="password"
                render={({ field: { onBlur, onChange, value }, fieldState: { error } }) => (
                  <AppTextInput
                    autoCapitalize="none"
                    autoComplete="current-password"
                    error={error?.message}
                    label="Şifre"
                    onBlur={onBlur}
                    onChangeText={onChange}
                    onSubmitEditing={() => void submitLogin()}
                    placeholder="Şifrenizi yazın"
                    returnKeyType="done"
                    secureTextEntry
                    textContentType="password"
                    value={value}
                  />
                )}
              />

              {submitError ? (
                <AppText accessibilityLiveRegion="assertive" tone="danger" variant="caption">
                  {submitError}
                </AppText>
              ) : null}

              {reason === 'password-reset' ? (
                <AppText accessibilityLiveRegion="polite" tone="brand" variant="caption">
                  Şifreniz yenilendi. Yeni şifrenizle giriş yapabilirsiniz.
                </AppText>
              ) : null}

              <AppButton
                icon={<KeyRound color={appTokens.brand[600]} size={18} />}
                label="Şifremi unuttum"
                onPress={() => router.push('/(auth)/sifre-sifirla')}
                variant="ghost"
              />

              <AppButton
                icon={<LogIn color={appTokens.content.onBrand} size={18} />}
                isLoading={isSubmitting}
                label="Giriş yap"
                onPress={() => void submitLogin()}
              />
            </AppCard>
          </View>
        </ScrollView>
      </KeyboardAvoidingView>
    </Screen>
  );
}
