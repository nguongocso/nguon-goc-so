import React from 'react';
import { Checkbox } from '@/components/ui/checkbox';
import { Badge } from '@/components/ui/badge';
import { ShieldAlert } from 'lucide-react';

/** Props cho thẻ hiển thị trường thông tin hồ sơ */
export interface ProfileFieldItemCardProps {
  fieldKey: string;
  fieldLabel: string;
  isChecked: boolean;
  isMandatory: boolean;
  disabled?: boolean;
  onToggle: () => void;
}

/** Component hiển thị từng thẻ trường dữ liệu hồ sơ (checkbox, nhãn, badge bắt buộc). */
export const ProfileFieldItemCard: React.FC<ProfileFieldItemCardProps> = ({
  fieldKey,
  fieldLabel,
  isChecked,
  isMandatory,
  disabled = false,
  onToggle,
}) => {
  return (
    <div
      className={`flex items-start gap-3 p-2.5 rounded-lg border transition-colors ${
        isMandatory
          ? 'bg-amber-50/40 border-amber-200/70 dark:bg-amber-950/10 dark:border-amber-800/40'
          : isChecked
            ? 'bg-primary/5 border-primary/20'
            : 'bg-card border-border hover:bg-muted/40'
      }`}
    >
      <Checkbox
        id={`field-${fieldKey}`}
        checked={isChecked}
        disabled={disabled || isMandatory}
        onCheckedChange={onToggle}
        className="mt-0.5"
      />
      <div className="flex-1 min-w-0">
        <label
          htmlFor={`field-${fieldKey}`}
          className={`text-sm font-medium leading-tight block ${
            isMandatory
              ? 'text-foreground cursor-not-allowed'
              : 'text-foreground cursor-pointer'
          }`}
        >
          {fieldLabel}
        </label>
        <div className="flex items-center gap-1.5 mt-1">
          <span className="text-xs text-muted-foreground font-mono truncate">
            {fieldKey}
          </span>
          {isMandatory && (
            <Badge
              variant="destructive"
              className="text-[10px] h-4 px-1.5 flex items-center gap-0.5"
            >
              <ShieldAlert className="size-2.5" />
              Bắt buộc
            </Badge>
          )}
        </div>
      </div>
    </div>
  );
};
