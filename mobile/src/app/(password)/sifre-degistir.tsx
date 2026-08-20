import { zodResolver } from '@hookform/resolvers/zod';
import { useRouter } from 'expo-router';
import ArrowLeft from 'lucide-react-native/icons/arrow-left';
import KeyRound from 'lucide-react-native/icons/key-round';
import LogOut from 'lucide-react-native/icons/log-out';
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

const passwordSchema = z
  .object({
    currentPassword: z.string().min(1, 'Mevcut şifrenizi yazın.'),
    newPassword: z
      .string()
      .min(8, 'Yeni şifre en az 8 karakter olmalı.')
      .regex(/[A-Za-z]/, 'Yeni şifre en az bir harf içermeli.')
      .regex(/\d/, 'Yeni şifre en az bir rakam içermeli.'),
    newPasswordConfirm: z.string().min(1, 'Yeni şifrenizi tekrar yazın.'),
  })
  .refine((values) => values.newPassword === values.newPasswordConfirm, {
    message: 'Yeni şifreler eşleşmiyor.',
    path: ['newPasswordConfirm'],
  });

type PasswordFormValues = z.infer<typeof passwordSchema>;

export default function PasswordChangeScreen() {
  const router = useRouter();
  const { changePassword, mustChangePassword, signOut } = useAuth();
  const [submitError, setSubmitError] = useState<string | null>(null);
  const {
    control,
    formState: { isSubmitting },
    handleSubmit,
    setError,
  } = useForm<PasswordFormValues>({
    defaultValues: {
      currentPassword: '',
      newPassword: '',
      newPasswordConfirm: '',
    },
    resolver: zodResolver(passwordSchema),
  });

  const submitPassword = handleSubmit(async ({ currentPassword, newPassword }) => {
    setSubmitError(null);

    try {
      await changePassword({ currentPassword, newPassword });
    } catch (error) {
      if (error instanceof ApiClientError) {
        if (error.code === 'PASSWORD_REUSED') {
          setError('newPassword', {
            message: 'Yeni şifreniz mevcut şifrenizle aynı olamaz.',
            type: 'server',
          });
          return;
        }

        if (error.status === 401 || error.code === 'INVALID_CREDENTIALS') {
          setError('currentPassword', {
            message: 'Mevcut şifreniz doğru değil.',
            type: 'server',
          });
          return;
        }
      }

      setSubmitError('Şifreniz değiştirilemedi. Lütfen tekrar deneyin.');
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
                <View className="size-14 items-center justify-center rounded-app-md bg-brand-100 dark:bg-brand-900/40">
                  <KeyRound color={appTokens.brand[600]} size={26} />
                </View>
                <View className="items-center gap-1">
                  <AppText accessibilityRole="header" variant="title">
                    Şifrenizi değiştirin
                  </AppText>
                  <AppText className="text-center" tone="muted">
                    {mustChangePassword
                      ? 'Hesabınıza devam etmek için geçici şifrenizi kişisel bir şifreyle yenileyin.'
                      : 'Mevcut şifrenizi doğrulayarak hesabınız için yeni bir şifre belirleyin.'}
                  </AppText>
                </View>
              </View>

              <View className="rounded-app-md bg-app-surface-strong p-4 dark:bg-app-surface-strong-dark">
                <AppText tone="muted" variant="caption">
                  Yeni şifreniz en az 8 karakter, bir harf ve bir rakam içermelidir.
                </AppText>
              </View>

              <Controller
                control={control}
                name="currentPassword"
                render={({ field: { onBlur, onChange, value }, fieldState: { error } }) => (
                  <AppTextInput
                    autoCapitalize="none"
                    autoComplete="current-password"
                    error={error?.message}
                    label="Mevcut şifre"
                    onBlur={onBlur}
                    onChangeText={onChange}
                    placeholder="Mevcut şifrenizi yazın"
                    secureTextEntry
                    textContentType="password"
                    value={value}
                  />
                )}
              />

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
                name="newPasswordConfirm"
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
                label="Şifreyi değiştir"
                onPress={() => void submitPassword()}
              />
              <AppButton
                disabled={isSubmitting}
                icon={
                  mustChangePassword ? (
                    <LogOut color={appTokens.brand[600]} size={18} />
                  ) : (
                    <ArrowLeft color={appTokens.brand[600]} size={18} />
                  )
                }
                label={mustChangePassword ? 'Farklı hesapla giriş yap' : 'Profile dön'}
                onPress={() => {
                  if (mustChangePassword) {
                    void signOut();
                    return;
                  }

                  router.replace('/(app)/profil');
                }}
                variant="ghost"
              />
            </AppCard>
          </View>
        </ScrollView>
      </KeyboardAvoidingView>
    </Screen>
  );
}
