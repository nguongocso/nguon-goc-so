import React from 'react';

import { Badge } from '@/components/ui/badge';
import {
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from '@/components/ui/accordion';
import { ProfileFieldItemCard } from './ProfileFieldItemCard';
import {
  getFieldKey,
  getFieldLabel,
  getFieldMandatory,
  getGroupKey,
  getGroupLabel,
} from './ProfileFieldSelector';

import type {
  FieldGroupDefinition,
  FieldSelectionItem,
} from '@/types/profileTemplate';

/** Thuộc tính cho từng nhóm trường trong nhóm thu gọn. */
export interface ProfileFieldGroupItemProps {
  group: FieldGroupDefinition;
  selectedMap: Map<string, FieldSelectionItem>;
  disabled?: boolean;
  onToggleField: (fieldKey: string, fieldGroup: string, isMandatory: boolean) => void;
  onSelectAllInGroup: (group: FieldGroupDefinition) => void;
  onDeselectOptionalInGroup: (group: FieldGroupDefinition) => void;
}

/** Hiển thị nhóm trường dữ liệu, nút chọn nhanh và danh sách trường. */
export const ProfileFieldGroupItem: React.FC<ProfileFieldGroupItemProps> = ({
  group,
  selectedMap,
  disabled = false,
  onToggleField,
  onSelectAllInGroup,
  onDeselectOptionalInGroup,
}) => {
  const groupKey = getGroupKey(group);
  const groupLabel = getGroupLabel(group);
  const fields = Array.isArray(group.fields) ? group.fields : [];

  const groupSelectedCount = fields.filter((f) =>
    selectedMap.has(getFieldKey(f))
  ).length;
  const totalInGroup = fields.length;
  const mandatoryInGroup = fields.filter((f) => getFieldMandatory(f)).length;

  return (
    <AccordionItem
      key={groupKey}
      value={groupKey}
      className="border border-border rounded-xl bg-card px-4 py-1"
    >
      <AccordionTrigger className="hover:no-underline py-3">
        <div className="flex flex-wrap items-center gap-2.5 text-left">
          <span className="font-semibold text-sm text-foreground">
            {groupLabel}
          </span>
          <Badge variant="outline" className="text-xs text-muted-foreground">
            {groupSelectedCount}/{totalInGroup} trường
          </Badge>
          {mandatoryInGroup > 0 && (
            <Badge variant="destructive" className="text-xs">
              {mandatoryInGroup} trường bắt buộc
            </Badge>
          )}
        </div>
      </AccordionTrigger>

      <AccordionContent className="pt-2 pb-4">
        <div className="flex items-center justify-end gap-2 mb-3 text-xs">
          <button
            type="button"
            onClick={() => onSelectAllInGroup(group)}
            disabled={disabled}
            className="text-primary hover:underline font-medium disabled:opacity-50 cursor-pointer"
          >
            Chọn tất cả
          </button>
          <span className="text-muted-foreground">|</span>
          <button
            type="button"
            onClick={() => onDeselectOptionalInGroup(group)}
            disabled={disabled}
            className="text-muted-foreground hover:text-foreground hover:underline disabled:opacity-50 cursor-pointer"
          >
            Bỏ chọn tùy chọn
          </button>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-2.5">
          {fields.map((field) => {
            const key = getFieldKey(field);
            const label = getFieldLabel(field);
            const isChecked = selectedMap.has(key);
            const isMandatory = getFieldMandatory(field);

            return (
              <ProfileFieldItemCard
                key={key}
                fieldKey={key}
                fieldLabel={label}
                isChecked={isChecked}
                isMandatory={isMandatory}
                disabled={disabled}
                onToggle={() =>
                  onToggleField(key, groupKey, isMandatory)
                }
              />
            );
          })}
        </div>
      </AccordionContent>
    </AccordionItem>
  );
};
