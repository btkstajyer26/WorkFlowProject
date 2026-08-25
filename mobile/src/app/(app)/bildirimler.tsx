import Bell from 'lucide-react-native/icons/bell';

import { TabPlaceholder } from '@/components/navigation/TabPlaceholder';

export default function NotificationsScreen() {
  return (
    <TabPlaceholder
      description="Bildirim listesi ilgili özellik çalışmasında bu alana eklenecek."
      icon={Bell}
      title="Bildirimler"
    />
  );
}
