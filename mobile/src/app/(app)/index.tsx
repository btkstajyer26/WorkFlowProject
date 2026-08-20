import LayoutDashboard from 'lucide-react-native/icons/layout-dashboard';

import { TabPlaceholder } from '@/components/navigation/TabPlaceholder';

export default function DashboardScreen() {
  return (
    <TabPlaceholder
      description="Dashboard içeriği ilgili özellik çalışmasında bu alana eklenecek."
      icon={LayoutDashboard}
      title="Panel"
    />
  );
}
