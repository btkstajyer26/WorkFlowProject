import Files from 'lucide-react-native/icons/files';

import { TabPlaceholder } from '@/components/navigation/TabPlaceholder';

export default function RecordsScreen() {
  return (
    <TabPlaceholder
      description="Kayıt listeleme ve arama akışı ilgili özellik çalışmasında eklenecek."
      icon={Files}
      title="Kayıtlar"
    />
  );
}
