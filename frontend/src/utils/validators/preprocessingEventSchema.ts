import { z } from "zod";
import { getLocalDateString } from "@/utils/dateTime";

const inputQuantity = z
  .number({ invalid_type_error: "Khối lượng vào phải là số" })
  .positive("Khối lượng vào sơ chế phải lớn hơn 0");

const outputQuantity = z
  .number({ invalid_type_error: "Khối lượng sau sơ chế phải là số" })
  .nonnegative("Khối lượng sau sơ chế phải lớn hơn hoặc bằng 0");

const preprocessingDate = z
  .string()
  .min(1, "Vui lòng chọn ngày sơ chế")
  .regex(/^\d{4}-\d{2}-\d{2}$/, "Ngày sơ chế không đúng định dạng")
  .refine(
    (value) => value <= getLocalDateString(),
    "Ngày sơ chế không được là ngày ở tương lai",
  );

const optionalText = (max: number, message: string) =>
  z.string().trim().max(max, message).optional();

const locationFields = {
  latitude: z.number().min(-90).max(90).optional(),
  longitude: z.number().min(-180).max(180).optional(),
};

const validateQuantities = (
  values: { inputQuantity: number; outputQuantity: number },
  context: z.RefinementCtx,
) => {
  if (values.outputQuantity > values.inputQuantity) {
    context.addIssue({
      code: z.ZodIssueCode.custom,
      path: ["outputQuantity"],
      message: "Khối lượng sau sơ chế không được lớn hơn khối lượng vào",
    });
  }
};

export const recordPreprocessingSchema = z
  .object({
    productionLotId: z.string().uuid("Vui lòng chọn lô sản xuất"),
    inputQuantity,
    outputQuantity,
    grade: optionalText(100, "Hạng phân loại không được vượt quá 100 ký tự"),
    processingMethod: optionalText(
      500,
      "Mô tả cách sơ chế không được vượt quá 500 ký tự",
    ),
    preprocessingDate,
    ...locationFields,
  })
  .superRefine(validateQuantities);

export const correctPreprocessingSchema = z
  .object({
    inputQuantity,
    outputQuantity,
    grade: optionalText(100, "Hạng phân loại không được vượt quá 100 ký tự"),
    processingMethod: optionalText(
      500,
      "Mô tả cách sơ chế không được vượt quá 500 ký tự",
    ),
    preprocessingDate,
    correctionReason: z
      .string()
      .trim()
      .min(1, "Lý do đính chính không được để trống")
      .max(500, "Lý do không được vượt quá 500 ký tự"),
    ...locationFields,
  })
  .superRefine(validateQuantities);

export type RecordPreprocessingFormValues = z.infer<
  typeof recordPreprocessingSchema
>;

export type CorrectPreprocessingFormValues = z.infer<
  typeof correctPreprocessingSchema
>;
