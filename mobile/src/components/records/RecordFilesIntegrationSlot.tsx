import { AppCard } from '@/components/ui/AppCard';
import { AppText } from '@/components/ui/AppText';

export function RecordFilesIntegrationSlot({ recordId }: { recordId: string }) {
  return (
    <AppCard className="gap-2 border-dashed">
      <AppText variant="heading">Dosyalar</AppText>
      <AppText tone="muted">
        Dosya yükleme, indirme ve önizleme bileşeni ilgili dosya çalışması
        tamamlandığında bu alana bağlanacak.
      </AppText>
      <AppText tone="muted" variant="caption">
        Kayıt: {recordId}
      </AppText>
    </AppCard>
  );
}
