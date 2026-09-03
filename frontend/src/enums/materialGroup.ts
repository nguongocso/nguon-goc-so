import { MaterialGroup } from '@/types/inputMaterial';

export { MaterialGroup };

export const MATERIAL_GROUP_LABELS: Record<MaterialGroup, string> = {
  [MaterialGroup.PESTICIDE]: 'Thuốc bảo vệ thực vật',
  [MaterialGroup.FERTILIZER]: 'Phân bón',
  [MaterialGroup.BIOLOGICAL]: 'Chế phẩm sinh học',
  [MaterialGroup.OTHER]: 'Khác',
};

export const MATERIAL_GROUP_VARIANTS: Record<
  MaterialGroup,
  { label: string; bgClass: string; textClass: string; borderClass: string }
> = {
  [MaterialGroup.PESTICIDE]: {
    label: 'Thuốc bảo vệ thực vật',
    bgClass: 'bg-red-500/10 dark:bg-red-500/20',
    textClass: 'text-red-700 dark:text-red-400',
    borderClass: 'border-red-200 dark:border-red-800',
  },
  [MaterialGroup.FERTILIZER]: {
    label: 'Phân bón',
    bgClass: 'bg-emerald-500/10 dark:bg-emerald-500/20',
    textClass: 'text-emerald-700 dark:text-emerald-400',
    borderClass: 'border-emerald-200 dark:border-emerald-800',
  },
  [MaterialGroup.BIOLOGICAL]: {
    label: 'Chế phẩm sinh học',
    bgClass: 'bg-teal-500/10 dark:bg-teal-500/20',
    textClass: 'text-teal-700 dark:text-teal-400',
    borderClass: 'border-teal-200 dark:border-teal-800',
  },
  [MaterialGroup.OTHER]: {
    label: 'Khác',
    bgClass: 'bg-blue-500/10 dark:bg-blue-500/20',
    textClass: 'text-blue-700 dark:text-blue-400',
    borderClass: 'border-blue-200 dark:border-blue-800',
  },
};
