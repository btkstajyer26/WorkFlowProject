import CirclePlus from 'lucide-react-native/icons/circle-plus';

import { TabPlaceholder } from '@/components/navigation/TabPlaceholder';

export default function CreateRecordScreen() {
  return (
    <TabPlaceholder
      description="Yeni kayıt oluşturma formu ilgili özellik çalışmasında eklenecek."
      icon={CirclePlus}
      title="Yeni Kayıt"
    />
  );
}
