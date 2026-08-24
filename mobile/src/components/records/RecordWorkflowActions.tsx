import { useState } from 'react';
import { Modal, Pressable, View } from 'react-native';

import { ApiClientError } from '@/api/errors';
import type { RecordDetail } from '@/api/records';
import type { CurrentUser } from '@/api/users';
import type { WorkflowAction } from '@/api/workflow';
import { AppButton } from '@/components/ui/AppButton';
import { AppCard } from '@/components/ui/AppCard';
import { AppText } from '@/components/ui/AppText';
import { AppTextInput } from '@/components/ui/AppTextInput';
import { useRecordWorkflow } from '@/query/workflow';

type ActionConfig = {
  action: WorkflowAction;
  label: string;
  requiresComment?: boolean;
};

function getAvailableActions(
  record: RecordDetail,
  user: CurrentUser,
): ActionConfig[] {
  if (user.roleName === 'CALISAN' && user.id === record.createdBy) {
    if (record.status === 'TASLAK') {
      return [{ action: 'GONDER', label: 'İncelemeye gönder' }];
    }
    if (record.status === 'DUZENLEME_BEKLIYOR') {
      return [{ action: 'TEKRAR_GONDER', label: 'Tekrar gönder' }];
    }
  }

  if (
    user.roleName === 'BASKAN_YARDIMCISI' &&
    record.status === 'BSK_YRD_INCELEMESINDE'
  ) {
    return [
      { action: 'BASKANA_ILET', label: 'Başkana ilet' },
      {
        action: 'CALISANA_GERI_GONDER',
        label: 'Çalışana geri gönder',
        requiresComment: true,
      },
    ];
  }

  if (
    user.roleName === 'BASKAN' &&
    record.status === 'BASKAN_INCELEMESINDE'
  ) {
    return [
      { action: 'ONAYLA', label: 'Onayla' },
      { action: 'REDDET', label: 'Reddet', requiresComment: true },
      {
        action: 'CALISANA_GERI_GONDER',
        label: 'Çalışana geri gönder',
        requiresComment: true,
      },
      {
        action: 'BASKAN_YARDIMCISINA_GERI_GONDER',
        label: 'Başkan yardımcısına geri gönder',
        requiresComment: true,
      },
    ];
  }

  return [];
}

export function RecordWorkflowActions({
  onActionSuccess,
  record,
  user,
}: {
  onActionSuccess?: (action: WorkflowAction) => void;
  record: RecordDetail;
  user: CurrentUser;
}) {
  const mutation = useRecordWorkflow(record.id);
  const [selectedAction, setSelectedAction] = useState<ActionConfig | null>(null);
  const [comment, setComment] = useState('');
  const [errorMessage, setErrorMessage] = useState('');
  const availableActions = getAvailableActions(record, user);

  const closeModal = () => {
    if (mutation.isPending) return;
    setSelectedAction(null);
    setComment('');
    setErrorMessage('');
  };

  const submitAction = async () => {
    if (!selectedAction) return;
    const normalizedComment = comment.trim();

    if (selectedAction.requiresComment && !normalizedComment) {
      setErrorMessage('Bu işlem için açıklama zorunludur.');
      return;
    }

    try {
      setErrorMessage('');
      await mutation.mutateAsync({
        action: selectedAction.action,
        ...(normalizedComment ? { comment: normalizedComment } : {}),
      });
      onActionSuccess?.(selectedAction.action);
      closeModal();
    } catch (error) {
      setErrorMessage(
        error instanceof ApiClientError
          ? error.message
          : 'İşlem tamamlanamadı. Lütfen tekrar deneyin.',
      );
    }
  };

  if (availableActions.length === 0) return null;

  return (
    <AppCard className="gap-3">
      <AppText variant="heading">Kayıt işlemleri</AppText>
      <View className="gap-2">
        {availableActions.map((config) => (
          <AppButton
            key={config.action}
            label={config.label}
            onPress={() => setSelectedAction(config)}
            variant={config.action === 'ONAYLA' ? 'primary' : 'secondary'}
          />
        ))}
      </View>

      <Modal
        animationType="fade"
        onRequestClose={closeModal}
        transparent
        visible={selectedAction !== null}
      >
        <View className="flex-1 justify-end bg-black/50 p-5">
          <Pressable className="absolute inset-0" onPress={closeModal} />
          <AppCard className="gap-4 p-5">
            <View className="gap-1">
              <AppText variant="heading">{selectedAction?.label}</AppText>
              <AppText tone="muted">
                {selectedAction?.requiresComment
                  ? 'Devam etmek için bir açıklama yazın.'
                  : 'İsterseniz işlem notu ekleyebilirsiniz.'}
              </AppText>
            </View>
            <AppTextInput
              className="min-h-28 py-3"
              error={errorMessage || undefined}
              label={selectedAction?.requiresComment ? 'Açıklama' : 'İşlem notu'}
              maxLength={2000}
              multiline
              onChangeText={(value) => {
                setComment(value);
                if (errorMessage) setErrorMessage('');
              }}
              placeholder="Açıklamanızı yazın"
              textAlignVertical="top"
              value={comment}
            />
            <AppButton
              isLoading={mutation.isPending}
              label="İşlemi onayla"
              onPress={() => void submitAction()}
            />
            <AppButton
              disabled={mutation.isPending}
              label="Vazgeç"
              onPress={closeModal}
              variant="secondary"
            />
          </AppCard>
        </View>
      </Modal>
    </AppCard>
  );
}
