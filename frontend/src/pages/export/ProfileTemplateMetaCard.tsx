import React from 'react';
import { Controller } from 'react-hook-form';
import type { Control, UseFormSetValue, UseFormWatch } from 'react-hook-form';
import { Building, AlertCircle, FileText } from 'lucide-react';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Switch } from '@/components/ui/switch';
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import { cn } from '@/lib/utils';

export interface ProfileTemplateFormData {
  name: string;
  partnerName?: string;
  isDefault: boolean;
}

/** Props cho thẻ nhập thông tin cơ bản của mẫu hồ sơ */
export interface ProfileTemplateMetaCardProps {
  control: Control<ProfileTemplateFormData>;
  setValue: UseFormSetValue<ProfileTemplateFormData>;
  watch: UseFormWatch<ProfileTemplateFormData>;
  submitting: boolean;
  isEdit: boolean;
  errors: {
    name?: { message?: string };
    partnerName?: { message?: string };
  };
  mandatoryValidationErr: string | null;
}

/**
 * Nhập tên mẫu, đối tác áp dụng và trạng thái mẫu mặc định.
 */
export const ProfileTemplateMetaCard: React.FC<ProfileTemplateMetaCardProps> = ({
  control,
  setValue,
  watch,
  submitting,
  isEdit,
  errors,
  mandatoryValidationErr,
}) => {
  return (
    <Card>
      <CardHeader>
        <div className="flex items-center gap-2 text-primary">
          <FileText className="size-5" />
          <CardTitle className="text-xl">
            {isEdit ? 'Chỉnh sửa mẫu hồ sơ truy xuất' : 'Tạo mẫu hồ sơ truy xuất mới'}
          </CardTitle>
        </div>
        <CardDescription>
          Thiết lập tên mẫu, đối tác mục tiêu và các trường dữ liệu được phép kết xuất.
        </CardDescription>
      </CardHeader>

      <CardContent className="space-y-4">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="space-y-2">
            <Label htmlFor="name">
              Tên mẫu hồ sơ <span className="text-destructive">*</span>
            </Label>
            <Controller
              name="name"
              control={control}
              render={({ field }) => (
                <Input
                  id="name"
                  placeholder="VD: Mẫu xuất khẩu Châu Âu, Mẫu siêu thị AEON..."
                  disabled={submitting}
                  {...field}
                />
              )}
            />
            {errors.name && (
              <p className="text-xs text-destructive">{errors.name.message}</p>
            )}
          </div>

          <div className="space-y-2">
            <Label htmlFor="partnerName">
              Đối tác áp dụng <span className="text-xs text-muted-foreground">(tùy chọn)</span>
            </Label>
            <Controller
              name="partnerName"
              control={control}
              render={({ field }) => (
                <div className="relative">
                  <Input
                    id="partnerName"
                    placeholder="VD: AEON Mall, Central Retail, WinCommerce..."
                    disabled={submitting}
                    {...field}
                  />
                  <Building className="absolute right-3 top-2.5 size-4 text-muted-foreground pointer-events-none" />
                </div>
              )}
            />
            {errors.partnerName && (
              <p className="text-xs text-destructive">{errors.partnerName.message}</p>
            )}
          </div>
        </div>

        <div
          className={cn(
            'flex items-center justify-between p-3.5 rounded-xl border border-border',
            'bg-muted/20 hover:bg-muted/30 transition-colors cursor-pointer select-none',
          )}
          onClick={() => {
            setValue('isDefault', !watch('isDefault'), {
              shouldValidate: true,
              shouldDirty: true,
            });
          }}
        >
          <div className="space-y-0.5 pointer-events-none">
            <Label htmlFor="isDefault" className="text-sm font-medium cursor-pointer">
              Đặt làm mẫu hồ sơ mặc định của tổ chức
            </Label>
            <p className="text-xs text-muted-foreground">
              Khi người xuất không chọn mẫu cụ thể, hệ thống sẽ tự động áp dụng mẫu mặc định này.
            </p>
          </div>
          <div onClick={(e) => e.stopPropagation()}>
            <Controller
              name="isDefault"
              control={control}
              render={({ field }) => (
                <Switch
                  id="isDefault"
                  checked={Boolean(field.value)}
                  onCheckedChange={(checked) => field.onChange(Boolean(checked))}
                  disabled={submitting}
                />
              )}
            />
          </div>
        </div>

        {mandatoryValidationErr && (
          <div
            className={cn(
              'flex items-start gap-2.5 p-3 rounded-lg border border-destructive/30',
              'bg-destructive/10 text-destructive text-sm',
            )}
          >
            <AlertCircle className="size-4 shrink-0 mt-0.5" />
            <div className="flex-1">{mandatoryValidationErr}</div>
          </div>
        )}
      </CardContent>
    </Card>
  );
};
