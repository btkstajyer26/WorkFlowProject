import { useState } from 'react';
import { ActivityIndicator, Modal, Pressable, ScrollView, View } from 'react-native';

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
  useWorkflowTargetDepartments,
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

  const [targetDepartmentId, setTargetDepartmentId] = useState<number | null>(null);
  const availableActions = availableActionsQuery.data?.actions ?? [];
  const targetDepartmentsQuery = useWorkflowTargetDepartments(
    record.id,
    Boolean(selectedAction?.targetDepartmentRequired && !selectedAction.targetUserRequired),
  );
  const departments = targetDepartmentsQuery.data?.departments ?? [];
  const targetUnavailable = Boolean(selectedAction?.targetUserRequired) || Boolean(
    selectedAction?.targetDepartmentRequired && (
      targetDepartmentsQuery.isPending || targetDepartmentsQuery.isFetching ||
      targetDepartmentsQuery.isError || departments.length === 0
    ),
  );

  const closeModal = () => {
    if (mutation.isPending) return;
    setSelectedAction(null);
    setTargetDepartmentId(null);
    setComment('');
    setErrorMessage('');
  };

  const submitAction = async () => {
    if (!selectedAction || mutation.isPending || targetUnavailable) return;
    if (selectedAction.targetDepartmentRequired &&
        !departments.some((department) => department.id === targetDepartmentId)) {
      setErrorMessage('Bir hedef departman seçin.');
      return;
    }
    const normalizedComment = comment.trim();

    if (selectedAction.commentRequired && !normalizedComment) {
      setErrorMessage('Bu işlem için açıklama zorunludur.');
      return;
    }

    try {
      setErrorMessage('');
      await mutation.mutateAsync({
        action: selectedAction.action,
        ...(selectedAction.targetDepartmentRequired && targetDepartmentId !== null
          ? { targetDepartmentId } : {}),
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
            onPress={() => {
              setTargetDepartmentId(null);
              setComment('');
              setErrorMessage('');
              setSelectedAction(action);
            }}
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
            {selectedAction?.targetUserRequired ? (
              <AppText tone="danger">
                Bu işlem için kullanıcı seçimi şu anda desteklenmiyor.
              </AppText>
            ) : selectedAction?.targetDepartmentRequired ? (
              <View className="gap-2">
                <AppText variant="label">Hedef departman</AppText>
                {targetDepartmentsQuery.isPending || targetDepartmentsQuery.isFetching ? (
                  <View className="gap-2">
                    <ActivityIndicator />
                    <AppText tone="muted">Departmanlar yükleniyor…</AppText>
                  </View>
                ) : targetDepartmentsQuery.isError ? (
                  <View className="gap-2">
                    <AppText tone="danger">Departmanlar yüklenemedi.</AppText>
                    <AppButton label="Departmanları yeniden yükle"
                      onPress={() => void targetDepartmentsQuery.refetch()} variant="secondary" />
                  </View>
                ) : departments.length === 0 ? (
                  <AppText tone="muted">Gönderilebilecek departman yok.</AppText>
                ) : (
                  <ScrollView style={{ maxHeight: 180 }} nestedScrollEnabled>
                    <View className="gap-2">
                      {departments.map((department) => (
                        <Pressable key={department.id} accessibilityRole="radio"
                          accessibilityState={{ selected: targetDepartmentId === department.id,
                            disabled: mutation.isPending }}
                          disabled={mutation.isPending}
                          className={`min-h-11 justify-center rounded-app-lg border px-4 py-2 ${
                            targetDepartmentId === department.id
                              ? 'border-brand-600 bg-brand-100 dark:border-brand-400 dark:bg-brand-900/40'
                              : 'border-app-border bg-app-surface-strong dark:border-app-border-dark dark:bg-app-surface-strong-dark'
                          }`}
                          onPress={() => {
                            setTargetDepartmentId(department.id);
                            setErrorMessage('');
                          }}>
                          <AppText>{department.name}</AppText>
                        </Pressable>
                      ))}
                    </View>
                  </ScrollView>
                )}
              </View>
            ) : null}
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
              disabled={targetUnavailable}
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
