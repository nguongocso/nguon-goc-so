import { z } from "zod";

export const recordWarehouseEntrySchema = z.object({
  productionLotId: z.string().min(1, "Vui lòng chọn lô sản xuất"),
  entryTime: z.string().min(1, "Vui lòng nhập thời điểm nhập kho"),
  warehouseName: z
    .string()
    .min(1, "Tên kho lưu trữ không được để trống")
    .max(255, "Tên kho lưu trữ không được vượt quá 255 ký tự"),
  storageCondition: z
    .string()
    .max(500, "Điều kiện bảo quản không được vượt quá 500 ký tự")
    .optional()
    .or(z.literal("")),
  notes: z
    .string()
    .max(1000, "Ghi chú không được vượt quá 1000 ký tự")
    .optional()
    .or(z.literal("")),
  latitude: z.number().optional(),
  longitude: z.number().optional(),
});

export type RecordWarehouseEntryFormValues = z.infer<typeof recordWarehouseEntrySchema>;

export const recordWarehouseExitSchema = z.object({
  productionLotId: z.string().min(1, "Vui lòng chọn lô sản xuất"),
  exitTime: z.string().min(1, "Vui lòng nhập thời điểm xuất kho"),
  destination: z
    .string()
    .max(255, "Nơi chuyển đến không được vượt quá 255 ký tự")
    .optional()
    .or(z.literal("")),
  notes: z
    .string()
    .max(1000, "Ghi chú không được vượt quá 1000 ký tự")
    .optional()
    .or(z.literal("")),
  latitude: z.number().optional(),
  longitude: z.number().optional(),
});

export type RecordWarehouseExitFormValues = z.infer<typeof recordWarehouseExitSchema>;
