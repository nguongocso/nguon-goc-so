import { z } from 'zod';

/**
 * Schema xác thực dữ liệu sự kiện nhập kho HTX
 */
export const recordWarehouseEntrySchema = z.object({
  shipmentId: z.string().min(1, 'Vui lòng chọn lô hàng'),
  entryTime: z.string().min(1, 'Vui lòng nhập thời điểm nhập kho'),
  warehouseName: z
    .string()
    .min(1, 'Tên kho lưu trữ không được để trống')
    .max(255, 'Tên kho lưu trữ không được vượt quá 255 ký tự'),
  storageCondition: z
    .string()
    .max(500, 'Điều kiện bảo quản không được vượt quá 500 ký tự')
    .optional()
    .or(z.literal('')),
  notes: z
    .string()
    .max(1000, 'Ghi chú không được vượt quá 1000 ký tự')
    .optional()
    .or(z.literal('')),
  latitude: z.number().optional(),
  longitude: z.number().optional(),
});

/**
 * Kiểu dữ liệu form ghi nhận nhập kho HTX
 */
export type RecordWarehouseEntryFormValues = z.infer<typeof recordWarehouseEntrySchema>;

/**
 * Schema xác thực dữ liệu sự kiện xuất kho HTX
 */
export const recordWarehouseExitSchema = z.object({
  shipmentId: z.string().min(1, 'Vui lòng chọn lô hàng'),
  exitTime: z.string().min(1, 'Vui lòng nhập thời điểm xuất kho'),
  destination: z
    .string()
    .max(255, 'Nơi chuyển đến không được vượt quá 255 ký tự')
    .optional()
    .or(z.literal('')),
  notes: z
    .string()
    .max(1000, 'Ghi chú không được vượt quá 1000 ký tự')
    .optional()
    .or(z.literal('')),
  latitude: z.number().optional(),
  longitude: z.number().optional(),
});

/**
 * Kiểu dữ liệu form ghi nhận xuất kho HTX
 */
export type RecordWarehouseExitFormValues = z.infer<typeof recordWarehouseExitSchema>;
