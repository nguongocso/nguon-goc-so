import React from 'react';
import { Controller } from 'react-hook-form';
import type { Control } from 'react-hook-form';

import { Label } from '@/components/ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';

import type { Organization } from '@/types/organization';
import type { ExportOpenDataFormValues } from '@/utils/validators';

/** Thuộc tính cho bộ lọc tổ chức dành cho Quản trị viên. */
export interface ExportOrganizationFilterProps {
  control: Control<ExportOpenDataFormValues>;
  organizations: Organization[];
  submitting: boolean;
  errorMessage?: string;
}

/** Thành phần lựa chọn tổ chức xuất dữ liệu dành riêng cho Quản trị viên hệ thống (VT-01). */
export const ExportOrganizationFilter: React.FC<ExportOrganizationFilterProps> = ({
  control,
  organizations,
  submitting,
  errorMessage,
}) => {
  return (
    <div className="space-y-2">
      <Label htmlFor="organizationId">Tổ chức</Label>
      <Controller
        name="organizationId"
        control={control}
        render={({ field }) => (
          <Select
            value={field.value ?? ''}
            onValueChange={(val) => field.onChange(val || undefined)}
            disabled={submitting}
          >
            <SelectTrigger id="organizationId">
              <SelectValue placeholder="Tất cả tổ chức" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">Tất cả</SelectItem>
              {organizations.map((org) => (
                <SelectItem key={org.id} value={org.id}>
                  {org.name} ({org.code})
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        )}
      />
      {errorMessage && (
        <p className="text-sm text-red-500">{errorMessage}</p>
      )}
    </div>
  );
};
