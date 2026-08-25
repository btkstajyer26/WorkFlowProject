import { useRouter } from 'expo-router';
import { ActivityIndicator, ScrollView, View } from 'react-native';

import { RecordForm } from '@/components/records/RecordForm';
import { AppButton } from '@/components/ui/AppButton';
import { AppCard } from '@/components/ui/AppCard';
import { AppText } from '@/components/ui/AppText';
import { Screen } from '@/components/ui/Screen';
import { useCategories } from '@/query/categories';
import { useCurrentUser } from '@/query/currentUser';
import { useCreateRecord } from '@/query/records';
import { appTokens } from '@/theme/theme';

export default function CreateRecordScreen() {
  const router = useRouter();
  const categoriesQuery = useCategories();
  const currentUserQuery = useCurrentUser();
  const createMutation = useCreateRecord();

  if (categoriesQuery.isPending || currentUserQuery.isPending) {
    return (
      <Screen className="items-center justify-center" edges={['left', 'right']}>
        <ActivityIndicator color={appTokens.brand[600]} size="large" />
        <AppText className="mt-3" tone="muted">
          Form hazırlanıyor…
        </AppText>
      </Screen>
    );
  }

  if (currentUserQuery.isError || currentUserQuery.data?.roleName !== 'CALISAN') {
    return (
      <Screen className="justify-center px-5" edges={['left', 'right']}>
        <AppCard className="gap-4">
          <AppText accessibilityRole="header" variant="heading">
            Yeni kayıt oluşturulamaz
          </AppText>
          <AppText tone="muted">
            Yeni kayıt oluşturma işlemi çalışan rolüne açıktır.
          </AppText>
          <AppButton
            label="Dashboard'a dön"
            onPress={() => router.replace('/')}
          />
        </AppCard>
      </Screen>
    );
  }

  return (
    <Screen edges={['left', 'right']}>
      <ScrollView
        contentContainerClassName="gap-5 px-5 py-5"
        keyboardShouldPersistTaps="handled"
      >
        <View className="gap-1">
          <AppText accessibilityRole="header" variant="title">
            Yeni kayıt
          </AppText>
          <AppText tone="muted">
            Bilgileri tamamlayıp kaydı taslak olarak oluşturun.
          </AppText>
        </View>

        {categoriesQuery.isError ? (
          <AppCard className="gap-3">
            <AppText tone="danger">Kategoriler yüklenemedi.</AppText>
            <AppText tone="muted">
              Kayıt oluşturabilmek için kategori listesinin yüklenmesi gerekir.
            </AppText>
            <AppButton
              label="Tekrar dene"
              onPress={() => void categoriesQuery.refetch()}
              variant="secondary"
            />
          </AppCard>
        ) : (
          <RecordForm
            categories={categoriesQuery.data ?? []}
            onSubmit={async (values) => {
              const record = await createMutation.mutateAsync(values);
              router.replace({
                params: { id: record.id },
                pathname: '/kayitlar/[id]',
              });
            }}
            submitLabel="Taslak oluştur"
          />
        )}

        <AppCard className="gap-2 border-dashed">
          <AppText variant="label">Dosya ekleme</AppText>
          <AppText tone="muted">
            Dosyalar, kayıt taslak olarak oluşturulduktan sonra kayıt detayından
            eklenecek.
          </AppText>
        </AppCard>
      </ScrollView>
    </Screen>
  );
}
