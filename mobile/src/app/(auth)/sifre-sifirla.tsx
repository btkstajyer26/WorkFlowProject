import { zodResolver } from '@hookform/resolvers/zod';
import { useRouter } from 'expo-router';
import ArrowLeft from 'lucide-react-native/icons/arrow-left';
import MailCheck from 'lucide-react-native/icons/mail-check';
import Send from 'lucide-react-native/icons/send';
import ShieldCheck from 'lucide-react-native/icons/shield-check';
import { useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { KeyboardAvoidingView, Platform, ScrollView, View } from 'react-native';
import { z } from 'zod';

import { forgotPassword, verifyResetCode } from '@/api/auth';
import { ApiClientError } from '@/api/errors';
import { AppButton } from '@/components/ui/AppButton';
import { AppCard } from '@/components/ui/AppCard';
import { AppText } from '@/components/ui/AppText';
import { AppTextInput } from '@/components/ui/AppTextInput';
import { Screen } from '@/components/ui/Screen';
import { appTokens } from '@/theme/theme';

const emailSchema = z.object({
  email: z.string().trim().min(1, 'E-posta adresinizi yazın.').email('Geçerli bir e-posta yazın.'),
});

const codeSchema = z.object({
  code: z.string().trim().regex(/^\d{6}$/, 'E-postanıza gelen 6 haneli kodu yazın.'),
});

type EmailFormValues = z.infer<typeof emailSchema>;
type CodeFormValues = z.infer<typeof codeSchema>;

function getRequestError(error: unknown, fallback: string): string {
  return error instanceof ApiClientError ? error.message : fallback;
}

export default function ForgotPasswordScreen() {
  const router = useRouter();
  const [requestedEmail, setRequestedEmail] = useState<string | null>(null);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const emailForm = useForm<EmailFormValues>({
    defaultValues: { email: '' },
    resolver: zodResolver(emailSchema),
  });
  const codeForm = useForm<CodeFormValues>({
    defaultValues: { code: '' },
    resolver: zodResolver(codeSchema),
  });

  const submitEmail = emailForm.handleSubmit(async ({ email }) => {
    setSubmitError(null);

    try {
      await forgotPassword({ email });
      setRequestedEmail(email);
    } catch (error) {
      setSubmitError(getRequestError(error, 'Doğrulama kodu gönderilemedi.'));
    }
  });

  const submitCode = codeForm.handleSubmit(async ({ code }) => {
    if (!requestedEmail) return;
    setSubmitError(null);

    try {
      const result = await verifyResetCode({ code, email: requestedEmail });
      router.replace({
        pathname: '/(auth)/yeni-sifre',
        params: { token: result.resetToken },
      });
    } catch (error) {
      setSubmitError(getRequestError(error, 'Kod doğrulanamadı.'));
    }
  });

  const isSubmitting = requestedEmail
    ? codeForm.formState.isSubmitting
    : emailForm.formState.isSubmitting;

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
                  {requestedEmail ? (
                    <MailCheck color={appTokens.brand[500]} size={28} />
                  ) : (
                    <ShieldCheck color={appTokens.brand[500]} size={28} />
                  )}
                </View>
                <View className="items-center gap-1">
                  <AppText accessibilityRole="header" variant="title">
                    {requestedEmail ? 'Kodunuzu doğrulayın' : 'Şifrenizi sıfırlayın'}
                  </AppText>
                  <AppText className="text-center" tone="muted">
                    {requestedEmail
                      ? `${requestedEmail} adresine gönderilen 6 haneli kodu yazın.`
                      : 'Hesabınıza bağlı e-posta adresini yazın; size bir doğrulama kodu gönderelim.'}
                  </AppText>
                </View>
              </View>

              {requestedEmail ? (
                <Controller
                  control={codeForm.control}
                  name="code"
                  render={({ field: { onBlur, onChange, value }, fieldState: { error } }) => (
                    <AppTextInput
                      autoComplete="one-time-code"
                      error={error?.message}
                      inputMode="numeric"
                      label="Doğrulama kodu"
                      maxLength={6}
                      onBlur={onBlur}
                      onChangeText={onChange}
                      onSubmitEditing={() => void submitCode()}
                      placeholder="000000"
                      returnKeyType="done"
                      textContentType="oneTimeCode"
                      value={value}
                    />
                  )}
                />
              ) : (
                <Controller
                  control={emailForm.control}
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
                      onSubmitEditing={() => void submitEmail()}
                      placeholder="ornek@kurum.gov.tr"
                      returnKeyType="send"
                      textContentType="username"
                      value={value}
                    />
                  )}
                />
              )}

              {submitError ? (
                <AppText accessibilityLiveRegion="assertive" tone="danger" variant="caption">
                  {submitError}
                </AppText>
              ) : null}

              <AppButton
                icon={
                  requestedEmail ? (
                    <ShieldCheck color={appTokens.content.onBrand} size={18} />
                  ) : (
                    <Send color={appTokens.content.onBrand} size={18} />
                  )
                }
                isLoading={isSubmitting}
                label={requestedEmail ? 'Kodu doğrula' : 'Kod gönder'}
                onPress={() => void (requestedEmail ? submitCode() : submitEmail())}
              />

              {requestedEmail ? (
                <AppButton
                  disabled={isSubmitting}
                  label="E-posta adresini değiştir"
                  onPress={() => {
                    setRequestedEmail(null);
                    setSubmitError(null);
                    codeForm.reset();
                  }}
                  variant="ghost"
                />
              ) : null}

              <AppButton
                disabled={isSubmitting}
                icon={<ArrowLeft color={appTokens.brand[600]} size={18} />}
                label="Giriş ekranına dön"
                onPress={() => router.back()}
                variant="ghost"
              />
            </AppCard>
          </View>
        </ScrollView>
      </KeyboardAvoidingView>
    </Screen>
  );
}
