import { z } from 'zod';

export const createSupplementSchema = z.object({
  requestedQuantity: z
    .number({ message: 'Số lượng đề nghị phải là số' })
    .int('Số lượng đề nghị phải là số nguyên')
    .min(1, 'Số lượng đề nghị phải lớn hơn 0'),
  reason: z
    .string()
    .trim()
    .min(1, 'Lý do đề nghị không được để trống')
    .max(1000, 'Lý do đề nghị không được vượt quá 1000 ký tự'),
  evidenceEventIds: z
    .array(z.string().uuid('ID sự kiện không hợp lệ'))
    .min(1, 'Phải chọn ít nhất một sự kiện thu hoạch hoặc sơ chế làm bằng chứng'),
});

export const approveSupplementSchema = z.object({
  approvedQuantity: z
    .number({ message: 'Số lượng thực cấp phải là số' })
    .int('Số lượng thực cấp phải là số nguyên')
    .min(1, 'Số lượng thực cấp phải lớn hơn 0'),
  remarks: z
    .string()
    .max(2000, 'Ghi chú không được vượt quá 2000 ký tự')
    .optional()
    .or(z.literal('')),
});

export const rejectSupplementSchema = z.object({
  rejectionReason: z
    .string()
    .trim()
    .min(1, 'Lý do từ chối không được để trống')
    .max(1000, 'Lý do từ chối không được vượt quá 1000 ký tự'),
});

export type CreateSupplementFormValues = z.infer<typeof createSupplementSchema>;
export type ApproveSupplementFormValues = z.infer<typeof approveSupplementSchema>;
export type RejectSupplementFormValues = z.infer<typeof rejectSupplementSchema>;
