import { z } from 'zod';

export const procurementEventSchema = z.object({
  shipmentId: z.string().min(1, 'Vui lòng nhập mã lô hàng').uuid('Mã lô hàng không hợp lệ (UUID)'),
  receivedQuantity: z.coerce.number().positive('Số lượng thực nhận phải lớn hơn 0'),
  notes: z.string().max(500, 'Ghi chú tối đa 500 ký tự').optional(),
  latitude: z.coerce.number().min(-90).max(90).optional(),
  longitude: z.coerce.number().min(-180).max(180).optional(),
});

export type ProcurementEventFormValues = z.infer<typeof procurementEventSchema>;