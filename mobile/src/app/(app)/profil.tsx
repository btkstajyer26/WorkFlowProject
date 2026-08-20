import UserRound from 'lucide-react-native/icons/user-round';

import { TabPlaceholder } from '@/components/navigation/TabPlaceholder';

export default function ProfileScreen() {
  return (
    <TabPlaceholder
      description="Profil ve hesap ayarları ilgili özellik çalışmasında bu alana eklenecek."
      icon={UserRound}
      title="Profil"
    />
  );
}
