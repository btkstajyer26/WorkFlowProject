import type { RecordStatus } from '@/api/records';

export const recordListViews = {
  taslaklar: { title: 'Taslaklarım', statuses: ['TASLAK'] },
  'duzeltme-bekleyenler': {
    title: 'Düzeltme bekleyen',
    statuses: ['DUZENLEME_BEKLIYOR'],
  },
  'onay-asamasindakiler': {
    title: 'Onay aşamasında',
    statuses: ['BSK_YRD_INCELEMESINDE', 'BASKAN_INCELEMESINDE'],
  },
  sonuclananlar: {
    title: 'Sonuçlananlar',
    statuses: ['ONAYLANDI', 'REDDEDILDI'],
  },
  incelenecekler: {
    title: 'İncelenecekler',
    statuses: ['BSK_YRD_INCELEMESINDE'],
  },
  'baskan-incelemesindekiler': {
    title: 'Başkan incelemesinde',
    statuses: ['BASKAN_INCELEMESINDE'],
  },
  'duzeltmede-olanlar': {
    title: 'Düzeltmede olanlar',
    statuses: ['DUZENLEME_BEKLIYOR'],
  },
  'onay-bekleyenler': {
    title: 'Onay bekleyenler',
    statuses: ['BASKAN_INCELEMESINDE'],
  },
  onaylananlar: { title: 'Onaylananlar', statuses: ['ONAYLANDI'] },
  reddedilenler: { title: 'Reddedilenler', statuses: ['REDDEDILDI'] },
} as const satisfies Record<
  string,
  { title: string; statuses: readonly RecordStatus[] }
>;

export type RecordListViewKey = keyof typeof recordListViews;

export function getRecordListView(value: string | string[] | undefined) {
  if (typeof value !== 'string') return undefined;
  if (!Object.hasOwn(recordListViews, value)) return undefined;

  return recordListViews[value as RecordListViewKey];
}
