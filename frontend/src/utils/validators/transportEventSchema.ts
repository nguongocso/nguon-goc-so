import { z } from "zod";

export const transportEventSchema = z.object({
  codeValue: z
    .string()
    .trim()
    .min(1, "Vui lòng quét hoặc nhập mã lô hàng."),

  fromLocation: z
    .string()
    .trim()
    .min(1, "Vui lòng nhập điểm đi."),

  toLocation: z
    .string()
    .trim()
    .min(1, "Vui lòng nhập điểm đến."),

  transportTime: z
    .string()
    .min(1, "Vui lòng chọn thời gian vận chuyển."),
});

export type TransportEventFormValues = z.infer<typeof transportEventSchema>;