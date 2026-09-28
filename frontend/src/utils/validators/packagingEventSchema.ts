import { z } from 'zod';

import { getLocalDateString } from '@/utils/dateTime';

/** Lược đồ xác thực dữ liệu sự kiện đóng gói. */
export const recordPackagingSchema = z.object({
  productionLotId: z.string().uuid('Vui lòng chọn lô sản xuất'),
  packagingSpecification: z
    .string()
    .min(1, 'Quy cách đóng gói không được để trống')
    .max(255, 'Quy cách đóng gói không được vượt quá 255 ký tự'),
  packagingDate: z
    .string()
    .regex(/^\d{4}-\d{2}-\d{2}$/, 'Ngày không đúng định dạng YYYY-MM-DD')
    .refine(
      (val) => val <= getLocalDateString(),
      'Ngày đóng gói không được là ngày ở tương lai'
    ),
  latitude: z.number().min(-90).max(90).optional(),
  longitude: z.number().min(-180).max(180).optional(),
});

/** Lược đồ xác thực dữ liệu đính chính sự kiện đóng gói. */
export const correctPackagingSchema = recordPackagingSchema
  .omit({ productionLotId: true })
  .extend({
    correctionReason: z
      .string()
      .min(1, 'Lý do đính chính không được để trống')
      .max(500, 'Lý do không được vượt quá 500 ký tự'),
  });

/** Kiểu dữ liệu form ghi nhận đóng gói. */
export type RecordPackagingFormValues = z.infer<typeof recordPackagingSchema>;

/** Kiểu dữ liệu form đính chính đóng gói. */
export type CorrectPackagingFormValues = z.infer<typeof correctPackagingSchema>;
