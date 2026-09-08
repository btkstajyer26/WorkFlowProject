import { useState } from 'react';
import { Modal, Pressable, View } from 'react-native';

import { ApiClientError } from '@/api/errors';
import type { RecordDetail } from '@/api/records';
import type {
  AvailableWorkflowAction,
  WorkflowAction,
} from '@/api/workflow';
import { AppButton } from '@/components/ui/AppButton';
import { AppCard } from '@/components/ui/AppCard';
import { AppText } from '@/components/ui/AppText';
import { AppTextInput } from '@/components/ui/AppTextInput';
import {
  useAvailableWorkflowActions,
  useRecordWorkflow,
} from '@/query/workflow';

export function RecordWorkflowActions({
  onActionSuccess,
  record,
}: {
  onActionSuccess?: (action: WorkflowAction) => void;
  record: RecordDetail;
}) {
  const mutation = useRecordWorkflow(record.id);
  const availableActionsQuery = useAvailableWorkflowActions(record.id);
  const [selectedAction, setSelectedAction] =
    useState<AvailableWorkflowAction | null>(null);
  const [comment, setComment] = useState('');
  const [errorMessage, setErrorMessage] = useState('');

  const availableActions =
    availableActionsQuery.data?.actions.filter(
      (action) =>
        !action.targetDepartmentRequired && !action.targetUserRequired,
    ) ?? [];

  const closeModal = () => {
    if (mutation.isPending) return;
    setSelectedAction(null);
    setComment('');
    setErrorMessage('');
  };

  const submitAction = async () => {
    if (!selectedAction) return;
    const normalizedComment = comment.trim();

    if (selectedAction.commentRequired && !normalizedComment) {
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

  if (
    availableActionsQuery.isPending ||
    availableActionsQuery.isError ||
    availableActions.length === 0
  ) {
    return null;
  }

  return (
    <AppCard className="gap-3">
      <AppText variant="heading">Kayıt işlemleri</AppText>
      <View className="gap-2">
        {availableActions.map((action) => (
          <AppButton
            key={action.action}
            label={action.displayName}
            onPress={() => setSelectedAction(action)}
            variant={action.action === 'ONAYLA' ? 'primary' : 'secondary'}
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
              <AppText variant="heading">
                {selectedAction?.displayName}
              </AppText>
              <AppText tone="muted">
                {selectedAction?.commentRequired
                  ? 'Devam etmek için bir açıklama yazın.'
                  : 'İsterseniz işlem notu ekleyebilirsiniz.'}
              </AppText>
            </View>
            <AppTextInput
              className="min-h-28 py-3"
              error={errorMessage || undefined}
              label={
                selectedAction?.commentRequired ? 'Açıklama' : 'İşlem notu'
              }
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
