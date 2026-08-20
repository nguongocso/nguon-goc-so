import { z } from 'zod';

export const recordPackagingSchema = z.object({
  productionLotId: z.string().uuid('Vui lòng chọn lô sản xuất'),
  packagingSpecification: z
    .string()
    .min(1, 'Quy cách đóng gói không được để trống')
    .max(255, 'Quy cách đóng gói không được vượt quá 255 ký tự'),
  packagingDate: z
    .string()
    .regex(/^\d{4}-\d{2}-\d{2}$/, 'Ngày không đúng định dạng YYYY-MM-DD')
    .refine((val) => new Date(val) <= new Date(), 'Ngày đóng gói không được là ngày ở tương lai'),
  latitude: z.number().min(-90).max(90).optional(),
  longitude: z.number().min(-180).max(180).optional(),
});

export const correctPackagingSchema = recordPackagingSchema
  .omit({ productionLotId: true })
  .extend({
    correctionReason: z
      .string()
      .min(1, 'Lý do đính chính không được để trống')
      .max(500, 'Lý do không được vượt quá 500 ký tự'),
  });

export type RecordPackagingFormValues = z.infer<typeof recordPackagingSchema>;
export type CorrectPackagingFormValues = z.infer<typeof correctPackagingSchema>;