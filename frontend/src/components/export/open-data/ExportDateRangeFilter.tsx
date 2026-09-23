import React, { useEffect, useState } from 'react';
import { Controller } from 'react-hook-form';
import type { Control, UseFormSetValue, UseFormWatch } from 'react-hook-form';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import type { ExportOpenDataFormValues } from '@/utils/validators';
import {
  toDateTimeLocal,
  detectActiveQuickRange,
  type QuickRangeKey,
} from './dateRangeHelpers';

export { toDateTimeLocal };

/** Props cho bộ lọc khoảng thời gian xuất dữ liệu */
export interface ExportDateRangeFilterProps {
  control: Control<ExportOpenDataFormValues>;
  setValue: UseFormSetValue<ExportOpenDataFormValues>;
  watch: UseFormWatch<ExportOpenDataFormValues>;
  submitting: boolean;
  errors: {
    fromDate?: { message?: string };
    toDate?: { message?: string };
  };
}

/**
 * Lọc thời gian theo mốc nhanh hoặc khoảng ngày tùy chọn.
 */
export const ExportDateRangeFilter: React.FC<ExportDateRangeFilterProps> = ({
  control,
  setValue,
  watch,
  submitting,
  errors,
}) => {
  const [activeQuickRange, setActiveQuickRange] = useState<QuickRangeKey>(null);
  const fromDate = watch('fromDate');
  const toDate = watch('toDate');

  useEffect(() => {
    setActiveQuickRange(detectActiveQuickRange(fromDate, toDate));
  }, [fromDate, toDate]);

  const setQuickRange = (days: number, key: QuickRangeKey) => {
    const now = new Date();
    const from = new Date(now);
    from.setDate(now.getDate() - days);
    setValue('fromDate', toDateTimeLocal(from, false));
    setValue('toDate', toDateTimeLocal(now, true));
    setActiveQuickRange(key);
  };

  const setThisWeek = () => {
    const now = new Date();
    const dayOfWeek = now.getDay();
    const diff = now.getDate() - dayOfWeek + (dayOfWeek === 0 ? -6 : 1);
    const monday = new Date(now);
    monday.setDate(diff);
    monday.setHours(0, 0, 0, 0);
    setValue('fromDate', toDateTimeLocal(monday, false));
    setValue('toDate', toDateTimeLocal(now, true));
    setActiveQuickRange('week');
  };

  const setThisMonth = () => {
    const now = new Date();
    const firstDay = new Date(now.getFullYear(), now.getMonth(), 1);
    setValue('fromDate', toDateTimeLocal(firstDay, false));
    setValue('toDate', toDateTimeLocal(now, true));
    setActiveQuickRange('month');
  };

  const setThisYear = () => {
    const now = new Date();
    const firstDay = new Date(now.getFullYear(), 0, 1);
    setValue('fromDate', toDateTimeLocal(firstDay, false));
    setValue('toDate', toDateTimeLocal(now, true));
    setActiveQuickRange('year');
  };

  const clearDates = () => {
    setValue('fromDate', undefined);
    setValue('toDate', undefined);
    setActiveQuickRange(null);
  };

  return (
    <>
      <div className="space-y-2">
        <Label>Khoảng thời gian</Label>
        <div className="flex flex-wrap gap-2">
          <Button
            type="button"
            variant={activeQuickRange === '7days' ? 'default' : 'outline'}
            size="sm"
            onClick={() => setQuickRange(7, '7days')}
            disabled={submitting}
          >
            7 ngày qua
          </Button>
          <Button
            type="button"
            variant={activeQuickRange === '30days' ? 'default' : 'outline'}
            size="sm"
            onClick={() => setQuickRange(30, '30days')}
            disabled={submitting}
          >
            30 ngày qua
          </Button>
          <Button
            type="button"
            variant={activeQuickRange === 'week' ? 'default' : 'outline'}
            size="sm"
            onClick={setThisWeek}
            disabled={submitting}
          >
            Tuần này
          </Button>
          <Button
            type="button"
            variant={activeQuickRange === 'month' ? 'default' : 'outline'}
            size="sm"
            onClick={setThisMonth}
            disabled={submitting}
          >
            Tháng này
          </Button>
          <Button
            type="button"
            variant={activeQuickRange === 'year' ? 'default' : 'outline'}
            size="sm"
            onClick={setThisYear}
            disabled={submitting}
          >
            Năm nay
          </Button>
          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={clearDates}
            disabled={submitting}
            className="text-muted-foreground"
          >
            Xóa
          </Button>
        </div>
      </div>

      <div className="grid grid-cols-2 gap-4">
        <div className="space-y-2">
          <Label htmlFor="fromDate">Từ ngày</Label>
          <Controller
            name="fromDate"
            control={control}
            render={({ field }) => (
              <Input
                id="fromDate"
                type="datetime-local"
                value={field.value ?? ''}
                onChange={field.onChange}
                disabled={submitting}
              />
            )}
          />
          {errors.fromDate && (
            <p className="text-sm text-red-500">{errors.fromDate.message}</p>
          )}
        </div>
        <div className="space-y-2">
          <Label htmlFor="toDate">Đến ngày</Label>
          <Controller
            name="toDate"
            control={control}
            render={({ field }) => (
              <Input
                id="toDate"
                type="datetime-local"
                value={field.value ?? ''}
                onChange={field.onChange}
                disabled={submitting}
              />
            )}
          />
          {errors.toDate && (
            <p className="text-sm text-red-500">{errors.toDate.message}</p>
          )}
        </div>
      </div>
    </>
  );
};
