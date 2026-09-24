import { z } from "zod";

/** Lược đồ kiểm tra biểu mẫu vùng trồng dùng chung cho tạo và sửa. */
export const farmAreaFormSchema = z.object({
  name: z.string().min(1, "Tên vùng trồng không được để trống").max(255),
  cropType: z.string().uuid("Vui lòng chọn loại cây trồng"),
  latitude: z.number({ required_error: "Vui lòng chọn vị trí trên bản đồ" }),
  longitude: z.number({ required_error: "Vui lòng chọn vị trí trên bản đồ" }),
  area: z
    .number({ invalid_type_error: "Vui lòng nhập diện tích" })
    .positive("Diện tích phải lớn hơn 0"),
  areaUnit: z.enum(["HA", "KM2", "M2", "SAO", "CONG", "MAU"], {
    required_error: "Vui lòng chọn đơn vị diện tích",
  }),
});

/** Giá trị biểu mẫu vùng trồng suy ra từ lược đồ. */
export type FarmAreaFormValues = z.infer<typeof farmAreaFormSchema>;
