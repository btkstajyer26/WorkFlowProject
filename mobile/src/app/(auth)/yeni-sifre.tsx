import { zodResolver } from '@hookform/resolvers/zod';
import { useLocalSearchParams, useRouter } from 'expo-router';
import KeyRound from 'lucide-react-native/icons/key-round';
import { useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { KeyboardAvoidingView, Platform, ScrollView, View } from 'react-native';
import { z } from 'zod';

import { resetPassword } from '@/api/auth';
import { ApiClientError } from '@/api/errors';
import { AppButton } from '@/components/ui/AppButton';
import { AppCard } from '@/components/ui/AppCard';
import { AppText } from '@/components/ui/AppText';
import { AppTextInput } from '@/components/ui/AppTextInput';
import { Screen } from '@/components/ui/Screen';
import { appTokens } from '@/theme/theme';

const newPasswordSchema = z
  .object({
    confirmPassword: z.string().min(1, 'Yeni şifrenizi tekrar yazın.'),
    newPassword: z
      .string()
      .min(8, 'Yeni şifre en az 8 karakter olmalıdır.')
      .regex(/[A-Za-zÇĞİÖŞÜçğıöşü]/, 'Yeni şifre en az bir harf içermelidir.')
      .regex(/\d/, 'Yeni şifre en az bir rakam içermelidir.'),
  })
  .refine((values) => values.newPassword === values.confirmPassword, {
    message: 'Yeni şifreler eşleşmiyor.',
    path: ['confirmPassword'],
  });

type NewPasswordFormValues = z.infer<typeof newPasswordSchema>;

export default function NewPasswordScreen() {
  const router = useRouter();
  const { token } = useLocalSearchParams<{ token?: string }>();
  const [submitError, setSubmitError] = useState<string | null>(null);
  const {
    control,
    formState: { isSubmitting },
    handleSubmit,
  } = useForm<NewPasswordFormValues>({
    defaultValues: { confirmPassword: '', newPassword: '' },
    resolver: zodResolver(newPasswordSchema),
  });

  const submitPassword = handleSubmit(async ({ newPassword }) => {
    setSubmitError(null);

    if (!token) {
      setSubmitError('Şifre sıfırlama bağlantısı geçersiz. Yeniden kod isteyin.');
      return;
    }

    try {
      await resetPassword({ newPassword, token });
      router.replace({ pathname: '/(auth)/giris', params: { reason: 'password-reset' } });
    } catch (error) {
      setSubmitError(
        error instanceof ApiClientError
          ? error.message
          : 'Şifre yenilenemedi. Lütfen tekrar deneyin.',
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
                <View className="size-14 items-center justify-center rounded-app-lg bg-brand-100 dark:bg-brand-900/40">
                  <KeyRound color={appTokens.brand[500]} size={28} />
                </View>
                <View className="items-center gap-1">
                  <AppText accessibilityRole="header" variant="title">
                    Yeni şifrenizi belirleyin
                  </AppText>
                  <AppText className="text-center" tone="muted">
                    En az 8 karakter, bir harf ve bir rakam içeren yeni bir şifre yazın.
                  </AppText>
                </View>
              </View>

              <Controller
                control={control}
                name="newPassword"
                render={({ field: { onBlur, onChange, value }, fieldState: { error } }) => (
                  <AppTextInput
                    autoCapitalize="none"
                    autoComplete="new-password"
                    error={error?.message}
                    label="Yeni şifre"
                    onBlur={onBlur}
                    onChangeText={onChange}
                    placeholder="Yeni şifrenizi yazın"
                    secureTextEntry
                    textContentType="newPassword"
                    value={value}
                  />
                )}
              />

              <Controller
                control={control}
                name="confirmPassword"
                render={({ field: { onBlur, onChange, value }, fieldState: { error } }) => (
                  <AppTextInput
                    autoCapitalize="none"
                    autoComplete="new-password"
                    error={error?.message}
                    label="Yeni şifre tekrar"
                    onBlur={onBlur}
                    onChangeText={onChange}
                    onSubmitEditing={() => void submitPassword()}
                    placeholder="Yeni şifrenizi tekrar yazın"
                    returnKeyType="done"
                    secureTextEntry
                    textContentType="newPassword"
                    value={value}
                  />
                )}
              />

              {submitError ? (
                <AppText accessibilityLiveRegion="assertive" tone="danger" variant="caption">
                  {submitError}
                </AppText>
              ) : null}

              <AppButton
                icon={<KeyRound color={appTokens.content.onBrand} size={18} />}
                isLoading={isSubmitting}
                label="Şifreyi yenile"
                onPress={() => void submitPassword()}
              />
            </AppCard>
          </View>
        </ScrollView>
      </KeyboardAvoidingView>
    </Screen>
  );
}
