import React, { useMemo } from 'react';
import { Checkbox } from '@/components/ui/checkbox';
import { Badge } from '@/components/ui/badge';
import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from '@/components/ui/accordion';
import { Card, CardContent } from '@/components/ui/card';
import { ShieldAlert, CheckCircle2 } from 'lucide-react';
import type {
  FieldGroupDefinition,
  FieldSelectionItem,
  AvailableFieldItem,
} from '@/types/profileTemplate';

interface ProfileFieldSelectorProps {
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

export const ProfileFieldSelector: React.FC<ProfileFieldSelectorProps> = ({
  availableGroups,
  selectedFields,
  onChange,
  disabled = false,
}) => {
  console.log('[ProfileFieldSelector] Render:', {
    groupsCount: availableGroups.length,
    selectedFieldsCount: selectedFields.length,
  });

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

  // Đếm thống kê
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

  // Xử lý toggle một trường
  const handleToggleField = (
    fieldKey: string,
    fieldGroup: string,
    isMandatory: boolean
  ) => {
    if (disabled || isMandatory) return; // Trường bắt buộc không được phép bỏ chọn (TC-02 UX)

    const resolvedGroup = fieldGroup || 'OTHER';

    if (selectedMap.has(fieldKey)) {
      // Bỏ chọn trường tùy chọn
      const next = selectedFields.filter((f) => f.fieldKey !== fieldKey);
      onChange(next);
    } else {
      // Chọn trường
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
    if (disabled) return;
    if (!Array.isArray(group.fields)) return;

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
                Gồm {mandatoryCount} trường bắt buộc (QTN-11) và {optionalCount} trường mở rộng tùy chọn
              </p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <Badge variant="destructive" className="text-xs font-medium">
              {mandatoryCount} Bắt buộc QTN-11
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
        {availableGroups.map((group) => {
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
                {/* Nút thao tác nhanh nhóm */}
                <div className="flex items-center justify-end gap-2 mb-3 text-xs">
                  <button
                    type="button"
                    onClick={() => handleSelectAllInGroup(group)}
                    disabled={disabled}
                    className="text-primary hover:underline font-medium disabled:opacity-50 cursor-pointer"
                  >
                    Chọn tất cả
                  </button>
                  <span className="text-muted-foreground">|</span>
                  <button
                    type="button"
                    onClick={() => handleDeselectOptionalInGroup(group)}
                    disabled={disabled}
                    className="text-muted-foreground hover:text-foreground hover:underline disabled:opacity-50 cursor-pointer"
                  >
                    Bỏ chọn tùy chọn
                  </button>
                </div>

                {/* Danh sách checkbox các trường */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-2.5">
                  {fields.map((field) => {
                    const key = getFieldKey(field);
                    const label = getFieldLabel(field);
                    const isChecked = selectedMap.has(key);
                    const isMandatory = getFieldMandatory(field);

                    return (
                      <div
                        key={key}
                        className={`flex items-start gap-3 p-2.5 rounded-lg border transition-colors ${
                          isMandatory
                            ? 'bg-amber-50/40 border-amber-200/70 dark:bg-amber-950/10 dark:border-amber-800/40'
                            : isChecked
                            ? 'bg-primary/5 border-primary/20'
                            : 'bg-card border-border hover:bg-muted/40'
                        }`}
                      >
                        <Checkbox
                          id={`field-${key}`}
                          checked={isChecked}
                          disabled={disabled || isMandatory}
                          onCheckedChange={() =>
                            handleToggleField(key, groupKey, isMandatory)
                          }
                          className="mt-0.5"
                        />
                        <div className="flex-1 min-w-0">
                          <label
                            htmlFor={`field-${key}`}
                            className={`text-sm font-medium leading-tight block ${
                              isMandatory
                                ? 'text-foreground cursor-not-allowed'
                                : 'text-foreground cursor-pointer'
                            }`}
                          >
                            {label}
                          </label>
                          <div className="flex items-center gap-1.5 mt-1">
                            <span className="text-xs text-muted-foreground font-mono truncate">
                              {key}
                            </span>
                            {isMandatory && (
                              <Badge
                                variant="destructive"
                                className="text-[10px] h-4 px-1.5 flex items-center gap-0.5"
                              >
                                <ShieldAlert className="size-2.5" />
                                Bắt buộc QTN-11
                              </Badge>
                            )}
                          </div>
                        </div>
                      </div>
                    );
                  })}
                </div>
              </AccordionContent>
            </AccordionItem>
          );
        })}
      </Accordion>
    </div>
  );
};
