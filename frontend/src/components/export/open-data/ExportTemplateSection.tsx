import React from 'react';
import { Controller } from 'react-hook-form';
import type { Control } from 'react-hook-form';
import { Link } from 'react-router-dom';
import { FileText, Settings, Eye } from 'lucide-react';
import { Label } from '@/components/ui/label';
import { Button } from '@/components/ui/button';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import type { ProfileTemplate } from '@/types/profileTemplate';
import type { ExportOpenDataFormValues } from '@/utils/validators';

/** Props cho phần lựa chọn mẫu hồ sơ xuất khẩu */
export interface ExportTemplateSectionProps {
  control: Control<ExportOpenDataFormValues>;
  profileTemplates: ProfileTemplate[];
  isManager: boolean;
  submitting: boolean;
  onOpenPreview: () => void;
}

/**
 * Component lựa chọn mẫu hồ sơ truy xuất theo đối tác (NCL-07-CN-007).
 * Cung cấp liên kết quản lý mẫu cho VT-02 và nút xem trước cấu trúc mẫu.
 */
export const ExportTemplateSection: React.FC<ExportTemplateSectionProps> = ({
  control,
  profileTemplates,
  isManager,
  submitting,
  onOpenPreview,
}) => {
  return (
    <div className="space-y-2 p-3.5 rounded-xl border border-border bg-card">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-1.5">
          <FileText className="size-4 text-primary" />
          <Label htmlFor="templateId" className="font-semibold text-sm">
            Mẫu hồ sơ truy xuất
          </Label>
        </div>
        {isManager && (
          <Link
            to="/export/profile-templates"
            className="text-xs text-primary hover:underline flex items-center gap-1"
          >
            <Settings className="size-3" />
            <span>Quản lý mẫu hồ sơ</span>
          </Link>
        )}
      </div>

      <div className="flex items-center gap-2">
        <div className="flex-1">
          <Controller
            name="templateId"
            control={control}
            render={({ field }) => (
              <Select
                value={field.value || 'default'}
                onValueChange={(val) =>
                  field.onChange(val === 'default' ? undefined : val)
                }
                disabled={submitting}
              >
                <SelectTrigger id="templateId">
                  <SelectValue placeholder="Chọn mẫu hồ sơ áp dụng" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="default" label="Dùng mẫu mặc định của tổ chức">
                    Dùng mẫu mặc định của tổ chức
                  </SelectItem>
                  {profileTemplates.map((tpl) => (
                    <SelectItem
                      key={tpl.id}
                      value={tpl.id}
                      label={
                        tpl.name +
                        (tpl.partnerName ? ` (${tpl.partnerName})` : '') +
                        (tpl.isDefault ? ' — [Mặc định]' : '')
                      }
                    >
                      {tpl.name}
                      {tpl.partnerName ? ` (${tpl.partnerName})` : ''}
                      {tpl.isDefault ? ' — [Mặc định]' : ''}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            )}
          />
        </div>

        <Button
          type="button"
          variant="outline"
          size="sm"
          onClick={onOpenPreview}
          className="gap-1.5 shrink-0"
          title="Xem trước cấu trúc hồ sơ theo mẫu"
        >
          <Eye className="size-4 text-primary" />
          <span className="hidden sm:inline">Xem trước</span>
        </Button>
      </div>

      <p className="text-xs text-muted-foreground">
        Tùy biến các trường dữ liệu đưa vào hồ sơ theo đúng yêu cầu biểu mẫu của đối tác thu mua (TC-01, TC-03).
      </p>
    </div>
  );
};
