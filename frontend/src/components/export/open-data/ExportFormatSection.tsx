import React from 'react';
import { Controller } from 'react-hook-form';
import type { Control } from 'react-hook-form';

import { Label } from '@/components/ui/label';

import type { ExportOpenDataFormValues } from '@/utils/validators';

/** Thuộc tính cho phần lựa chọn định dạng tệp xuất. */
export interface ExportFormatSectionProps {
  control: Control<ExportOpenDataFormValues>;
  selectedFormat: string;
  submitting: boolean;
  errorMessage?: string;
}

/** Thành phần lựa chọn định dạng tệp xuất dữ liệu mở (JSON, CSV, XML). */
export const ExportFormatSection: React.FC<ExportFormatSectionProps> = ({
  control,
  selectedFormat,
  submitting,
  errorMessage,
}) => {
  return (
    <div className="space-y-2">
      <Label>Định dạng *</Label>
      <Controller
        name="format"
        control={control}
        render={({ field }) => (
          <div className="flex gap-4">
            <label className="flex items-center gap-2 cursor-pointer">
              <input
                type="radio"
                value="JSON"
                checked={field.value === 'JSON'}
                onChange={() => field.onChange('JSON')}
                disabled={submitting}
              />
              JSON
            </label>
            <label className="flex items-center gap-2 cursor-pointer">
              <input
                type="radio"
                value="CSV"
                checked={field.value === 'CSV'}
                onChange={() => field.onChange('CSV')}
                disabled={submitting}
              />
              CSV
            </label>
            <label className="flex items-center gap-2 cursor-pointer">
              <input
                type="radio"
                value="XML"
                checked={field.value === 'XML'}
                onChange={() => field.onChange('XML')}
                disabled={submitting}
              />
              XML (chưa hỗ trợ)
            </label>
          </div>
        )}
      />
      {errorMessage && (
        <p className="text-sm text-red-500">{errorMessage}</p>
      )}
      {selectedFormat === 'XML' && (
        <p className="text-sm text-yellow-600">
          ⚠️ Định dạng XML chưa được hỗ trợ. Vui lòng chọn JSON hoặc CSV.
        </p>
      )}
    </div>
  );
};
