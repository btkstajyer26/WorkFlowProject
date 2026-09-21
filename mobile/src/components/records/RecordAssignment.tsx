import { View } from 'react-native';

import type { RecordAssignment as RecordAssignmentModel } from '@/api/records';
import { AppText } from '@/components/ui/AppText';

type RecordAssignmentProps = {
  assignment?: RecordAssignmentModel | null;
};

export function RecordAssignment({ assignment }: RecordAssignmentProps) {
  if (!assignment || assignment.kind === 'NONE') return null;

  const isDepartment = assignment.kind === 'DEPARTMENT';
  const displayName = isDepartment
    ? assignment.departmentName?.trim() ||
      (assignment.departmentId == null
        ? 'Departman bilgisi yok'
        : `Departman #${assignment.departmentId}`)
    : assignment.userFullName?.trim() ||
      assignment.userId ||
      'Kullanıcı bilgisi yok';

  return (
    <View className="gap-1">
      <AppText tone="muted" variant="caption">
        {isDepartment ? 'Atanan departman' : 'Atanan kullanıcı'}
      </AppText>
      <AppText>{displayName}</AppText>
    </View>
  );
}
