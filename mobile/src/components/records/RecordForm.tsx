import { zodResolver } from '@hookform/resolvers/zod';
import { Controller, useForm, useWatch } from 'react-hook-form';
import { Pressable, View } from 'react-native';
import { z } from 'zod';

import type { Category } from '@/api/categories';
import { ApiClientError } from '@/api/errors';
import type { RecordMutationRequest } from '@/api/records';
import { AppButton } from '@/components/ui/AppButton';
import { AppCard } from '@/components/ui/AppCard';
import { AppText } from '@/components/ui/AppText';
import { AppTextInput } from '@/components/ui/AppTextInput';

const recordFormSchema = z.object({
  categoryId: z.number().int().positive('Bir kategori seçin.'),
  description: z.string().trim().min(1, 'Açıklama zorunludur.'),
  title: z
    .string()
    .trim()
    .min(1, 'Başlık zorunludur.')
    .max(255, 'Başlık en fazla 255 karakter olabilir.'),
});

type RecordFormValues = z.infer<typeof recordFormSchema>;

type RecordFormProps = {
  categories: Category[];
  initialValues?: RecordMutationRequest;
  onCancel?: () => void;
  onSubmit: (values: RecordMutationRequest) => Promise<void>;
  submitLabel: string;
};

export function RecordForm({
  categories,
  initialValues,
  onCancel,
  onSubmit,
  submitLabel,
}: RecordFormProps) {
  const {
    control,
    formState: { errors, isSubmitting },
    handleSubmit,
    setError,
    setValue,
  } = useForm<RecordFormValues>({
    defaultValues: initialValues ?? {
      categoryId: 0,
      description: '',
      title: '',
    },
    resolver: zodResolver(recordFormSchema),
  });
  const selectedCategoryId = useWatch({ control, name: 'categoryId' });

  const submit = handleSubmit(async (values) => {
    try {
      await onSubmit(values);
    } catch (error) {
      if (error instanceof ApiClientError) {
        for (const fieldError of error.fieldErrors) {
          if (
            fieldError.field === 'title' ||
            fieldError.field === 'description' ||
            fieldError.field === 'categoryId'
          ) {
            setError(fieldError.field, { message: fieldError.message });
          }
        }
        setError('root.server', { message: error.message });
        return;
      }

      setError('root.server', {
        message: 'İşlem tamamlanamadı. Lütfen tekrar deneyin.',
      });
    }
  });

  return (
    <AppCard className="gap-5 p-5">
      <Controller
        control={control}
        name="title"
        render={({ field: { onBlur, onChange, value } }) => (
          <AppTextInput
            autoCapitalize="sentences"
            error={errors.title?.message}
            label="Başlık"
            maxLength={255}
            onBlur={onBlur}
            onChangeText={onChange}
            placeholder="Kayıt başlığını yazın"
            returnKeyType="next"
            value={value}
          />
        )}
      />

      <View className="gap-2">
        <AppText variant="label">Kategori</AppText>
        <View className="flex-row flex-wrap gap-2">
          {categories.map((category) => {
            const selected = selectedCategoryId === category.id;

            return (
              <Pressable
                accessibilityRole="radio"
                accessibilityState={{ selected }}
                className={`min-h-11 justify-center rounded-app-pill border px-4 ${
                  selected
                    ? 'border-brand-600 bg-brand-100 dark:border-brand-400 dark:bg-brand-900/40'
                    : 'border-app-border bg-app-surface-strong dark:border-app-border-dark dark:bg-app-surface-strong-dark'
                }`}
                key={category.id}
                onPress={() =>
                  setValue('categoryId', category.id, {
                    shouldDirty: true,
                    shouldValidate: true,
                  })
                }
              >
                <AppText tone={selected ? 'brand' : 'muted'} variant="label">
                  {category.name}
                </AppText>
              </Pressable>
            );
          })}
        </View>
        {errors.categoryId?.message ? (
          <AppText tone="danger" variant="caption">
            {errors.categoryId.message}
          </AppText>
        ) : null}
      </View>

      <Controller
        control={control}
        name="description"
        render={({ field: { onBlur, onChange, value } }) => (
          <AppTextInput
            className="min-h-32 py-3"
            error={errors.description?.message}
            label="Açıklama"
            multiline
            onBlur={onBlur}
            onChangeText={onChange}
            placeholder="Kayıt açıklamasını yazın"
            textAlignVertical="top"
            value={value}
          />
        )}
      />

      {errors.root?.server?.message ? (
        <AppText accessibilityLiveRegion="polite" tone="danger">
          {errors.root.server.message}
        </AppText>
      ) : null}

      <View className="gap-3">
        <AppButton
          isLoading={isSubmitting}
          label={submitLabel}
          onPress={() => void submit()}
        />
        {onCancel ? (
          <AppButton
            disabled={isSubmitting}
            label="Vazgeç"
            onPress={onCancel}
            variant="secondary"
          />
        ) : null}
      </View>
    </AppCard>
  );
}
