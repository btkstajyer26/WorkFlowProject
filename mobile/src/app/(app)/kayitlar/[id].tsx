import { useLocalSearchParams } from 'expo-router';
import { FileText } from 'lucide-react-native';

import { TabPlaceholder } from '@/components/navigation/TabPlaceholder';

export default function RecordDetailScreen() {
  const { id } = useLocalSearchParams<{ id?: string | string[] }>();
  const recordId = Array.isArray(id) ? id[0] : id;

  return (
    <TabPlaceholder
      description={
        recordId
          ? `${recordId} numaralı kaydın detayları ve işlem geçmişi bu alana eklenecek.`
          : 'Kayıt detayları ve işlem geçmişi bu alana eklenecek.'
      }
      icon={FileText}
      title="Kayıt Detayı"
    />
  );
}
