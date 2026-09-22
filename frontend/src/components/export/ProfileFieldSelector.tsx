import React, { useMemo } from 'react';
import { Badge } from '@/components/ui/badge';
import { Accordion } from '@/components/ui/accordion';
import { Card, CardContent } from '@/components/ui/card';
import { CheckCircle2 } from 'lucide-react';
import { ProfileFieldGroupItem } from './ProfileFieldGroupItem';
import type {
  FieldGroupDefinition,
  FieldSelectionItem,
  AvailableFieldItem,
} from '@/types/profileTemplate';

/** Thuộc tính props đầu vào cho component chọn trường hồ sơ */
export interface ProfileFieldSelectorProps {
  availableGroups: FieldGroupDefinition[];
  selectedFields: FieldSelectionItem[];
  onChange: (fields: FieldSelectionItem[]) => void;
  disabled?: boolean;
}

/** Helper trích xuất khóa định danh trường */
export const getFieldKey = (f: AvailableFieldItem): string => f.fieldKey || f.key || '';

/** Helper trích xuất tên hiển thị tiếng Việt của trường */
export const getFieldLabel = (f: AvailableFieldItem): string =>
  f.displayName || f.label || getFieldKey(f);

/** Helper kiểm tra trường có bắt buộc theo QTN-11 không */
export const getFieldMandatory = (f: AvailableFieldItem): boolean =>
  Boolean(f.mandatory ?? f.isMandatory);

/** Helper trích xuất mã nhóm trường */
export const getGroupKey = (g: FieldGroupDefinition): string =>
  g.fieldGroup || g.group || '';

/** Helper trích xuất tên nhóm trường */
export const getGroupLabel = (g: FieldGroupDefinition): string =>
  g.groupLabel || getGroupKey(g);

/**
 * Component lựa chọn các trường dữ liệu cho mẫu hồ sơ xuất khẩu.
 * Cho phép người dùng bật/tắt các trường tùy chọn, cố định trường bắt buộc theo QTN-11.
 */
export const ProfileFieldSelector: React.FC<ProfileFieldSelectorProps> = ({
  availableGroups,
  selectedFields,
  onChange,
  disabled = false,
}) => {
  // Map lưu trữ fieldKey -> FieldSelectionItem đã chọn
  const selectedMap = useMemo(() => {
    const map = new Map<string, FieldSelectionItem>();
    selectedFields.forEach((f) => {
      if (f.fieldKey) {
        map.set(f.fieldKey, f);
      }
    });
    return map;
  }, [selectedFields]);

  // Đếm thống kê số lượng trường bắt buộc và tùy chọn
  const { mandatoryCount, optionalCount, totalCount } = useMemo(() => {
    let mandatory = 0;
    let optional = 0;
    availableGroups.forEach((group) => {
      if (Array.isArray(group.fields)) {
        group.fields.forEach((f) => {
          const key = getFieldKey(f);
          if (selectedMap.has(key)) {
            if (getFieldMandatory(f)) {
              mandatory++;
            } else {
              optional++;
            }
          }
        });
      }
    });
    return {
      mandatoryCount: mandatory,
      optionalCount: optional,
      totalCount: mandatory + optional,
    };
  }, [availableGroups, selectedMap]);

  // Xử lý bật/tắt chọn một trường
  const handleToggleField = (
    fieldKey: string,
    fieldGroup: string,
    isMandatory: boolean
  ) => {
    if (disabled || isMandatory) return;

    const resolvedGroup = fieldGroup || 'OTHER';

    if (selectedMap.has(fieldKey)) {
      const next = selectedFields.filter((f) => f.fieldKey !== fieldKey);
      onChange(next);
    } else {
      const next = [
        ...selectedFields,
        {
          fieldKey,
          fieldGroup: resolvedGroup,
          isMandatory: false,
          sortOrder: selectedFields.length + 1,
        },
      ];
      onChange(next);
    }
  };

  // Chọn toàn bộ trường trong một nhóm
  const handleSelectAllInGroup = (group: FieldGroupDefinition) => {
    if (disabled) return;
    const currentKeys = new Set(selectedFields.map((f) => f.fieldKey));
    const newItems: FieldSelectionItem[] = [];
    const groupKey = getGroupKey(group);

    if (Array.isArray(group.fields)) {
      group.fields.forEach((f) => {
        const key = getFieldKey(f);
        if (key && !currentKeys.has(key)) {
          newItems.push({
            fieldKey: key,
            fieldGroup: groupKey,
            isMandatory: getFieldMandatory(f),
            sortOrder: selectedFields.length + newItems.length + 1,
          });
        }
      });
    }

    if (newItems.length > 0) {
      onChange([...selectedFields, ...newItems]);
    }
  };

  // Bỏ chọn các trường tùy chọn trong nhóm (giữ lại trường bắt buộc)
  const handleDeselectOptionalInGroup = (group: FieldGroupDefinition) => {
    if (disabled || !Array.isArray(group.fields)) return;

    const optionalKeysInGroup = new Set(
      group.fields.filter((f) => !getFieldMandatory(f)).map((f) => getFieldKey(f))
    );
    const next = selectedFields.filter((f) => !optionalKeysInGroup.has(f.fieldKey));
    onChange(next);
  };

  return (
    <div className="space-y-4">
      {/* Banner tóm tắt số lượng trường */}
      <Card className="border-border bg-muted/30">
        <CardContent className="p-4 flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="size-5 text-primary" />
            <div>
              <span className="font-semibold text-foreground text-sm">
                Tổng cộng {totalCount} trường được chọn
              </span>
              <p className="text-xs text-muted-foreground">
                Gồm {mandatoryCount} trường bắt buộc và {optionalCount} trường mở rộng tùy chọn
              </p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <Badge variant="destructive" className="text-xs font-medium">
              {mandatoryCount} Bắt buộc
            </Badge>
            <Badge variant="secondary" className="text-xs font-medium">
              {optionalCount} Tùy chọn
            </Badge>
          </div>
        </CardContent>
      </Card>

      {/* Accordion các nhóm trường */}
      <Accordion
        defaultValue={availableGroups.map((g) => getGroupKey(g))}
        className="w-full space-y-3"
      >
        {availableGroups.map((group) => (
          <ProfileFieldGroupItem
            key={getGroupKey(group)}
            group={group}
            selectedMap={selectedMap}
            disabled={disabled}
            onToggleField={handleToggleField}
            onSelectAllInGroup={handleSelectAllInGroup}
            onDeselectOptionalInGroup={handleDeselectOptionalInGroup}
          />
        ))}
      </Accordion>
    </div>
  );
};
