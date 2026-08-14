import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { getRoles } from "@/api/memberApi";
import type { RoleOption } from "@/types/member";
import { getRoleLabel } from "@/config/roleAccess";
import type { OrganizationType } from "@/types/auth";

const COOPERATIVE_ROLE_CODES = ["VT-02", "VT-03"] as const;
const AUTO_ROLE_BY_ORGANIZATION_TYPE: Partial<Record<OrganizationType, string>> = {
  SYSTEM: "VT-01",
  ENTERPRISE: "VT-04",
  GOVERNMENT: "VT-05",
};

const createOrgMemberSchema = z.object({
  username: z
    .string()
    .min(4, "Tên đăng nhập phải có từ 4 đến 30 ký tự")
    .max(30, "Tên đăng nhập phải có từ 4 đến 30 ký tự")
    .regex(
      /^[a-zA-Z0-9_-]+$/,
      "Tên đăng nhập chỉ chứa chữ cái, chữ số, dấu gạch ngang và dấu gạch dưới",
    ),
  password: z
    .string()
    .min(8, "Mật khẩu phải có từ 8 đến 50 ký tự")
    .max(50, "Mật khẩu phải có từ 8 đến 50 ký tự")
    .regex(
      /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]+$/,
      "Mật khẩu phải chứa ít nhất một chữ hoa, một chữ thường, một số và một ký tự đặc biệt",
    ),
  fullName: z.string().min(1, "Họ tên không được để trống"),
  email: z.string().email("Email không hợp lệ").or(z.literal("")),
  phone: z.string().optional().nullable().or(z.literal("")),
  roleId: z.number({ required_error: "Vai trò là bắt buộc" }).min(1, "Vui lòng chọn vai trò"),
});

export type CreateOrganizationMemberFormData = z.infer<typeof createOrgMemberSchema>;

interface Props {
  onSubmit: (data: CreateOrganizationMemberFormData) => void;
  loading?: boolean;
  organizationType: OrganizationType;
}

export function CreateOrganizationMemberForm({ onSubmit, loading = false, organizationType }: Props) {
  const [roleOptions, setRoleOptions] = useState<RoleOption[]>([]);

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    formState: { errors },
  } = useForm<CreateOrganizationMemberFormData>({
    resolver: zodResolver(createOrgMemberSchema),
    defaultValues: {
      username: "",
      password: "",
      fullName: "",
      email: "",
      phone: "",
      roleId: 0,
    },
  });

  useEffect(() => {
    const fixedRoleCode = AUTO_ROLE_BY_ORGANIZATION_TYPE[organizationType];

    if (fixedRoleCode) {
      const loadAutoRole = async () => {
        const roles = await getRoles();
        const matched = roles.find((role) => role.code === fixedRoleCode);
        setValue("roleId", matched?.roleId ?? 0);
      };

      loadAutoRole();
      return;
    }

    const loadRoles = async () => {
      const roles = await getRoles();
      setRoleOptions(roles.filter((role) => COOPERATIVE_ROLE_CODES.includes(role.code as (typeof COOPERATIVE_ROLE_CODES)[number])));
    };

    loadRoles();
  }, [organizationType, setValue]);

  const selectedRole = roleOptions.find((role) => role.roleId === watch("roleId"));
  const selectedRoleLabel = selectedRole ? getRoleLabel(selectedRole.code) : "";
  const autoRoleLabel = AUTO_ROLE_BY_ORGANIZATION_TYPE[organizationType]
    ? getRoleLabel(AUTO_ROLE_BY_ORGANIZATION_TYPE[organizationType])
    : "";
  const isCooperative = organizationType === "COOPERATIVE";

  return (
    <form className="space-y-8" onSubmit={handleSubmit(onSubmit)}>
      <div className="grid grid-cols-1 md:grid-cols-2 gap-x-8 gap-y-6">
        <div>
          <Label className="block text-base font-medium mb-1">Tên đăng nhập *</Label>
          <Input className="w-full h-11 text-base" {...register("username")} />
          {errors.username && <p className="text-sm text-red-500 mt-1">{errors.username.message}</p>}
        </div>

        <div>
          <Label className="block text-base font-medium mb-1">Mật khẩu *</Label>
          <Input className="w-full h-11 text-base" type="password" {...register("password")} />
          {errors.password && <p className="text-sm text-red-500 mt-1">{errors.password.message}</p>}
        </div>

        <div>
          <Label className="block text-base font-medium mb-1">Họ và tên *</Label>
          <Input className="w-full h-11 text-base" {...register("fullName")} />
          {errors.fullName && <p className="text-sm text-red-500 mt-1">{errors.fullName.message}</p>}
        </div>

        <div>
          <Label className="block text-base font-medium mb-1">Email</Label>
          <Input className="w-full h-11 text-base" type="email" {...register("email")} />
          {errors.email && <p className="text-sm text-red-500 mt-1">{errors.email.message}</p>}
        </div>

        <div>
          <Label className="block text-base font-medium mb-1">Số điện thoại</Label>
          <Input className="w-full h-11 text-base" {...register("phone")} />
          {errors.phone && <p className="text-sm text-red-500 mt-1">{errors.phone.message}</p>}
        </div>

        {isCooperative ? (
          <div>
            <Label className="block text-base font-medium mb-1">Vai trò *</Label>
            <Select
              value={selectedRoleLabel}
              onValueChange={(selectedLabel) => {
                const role = roleOptions.find((item) => getRoleLabel(item.code) === selectedLabel);
                setValue("roleId", role?.roleId ?? 0);
              }}
            >
              <SelectTrigger className="w-full h-11 text-base">
                <SelectValue placeholder="Chọn vai trò" />
              </SelectTrigger>
              <SelectContent side="bottom" align="start" className="z-[100]">
                {roleOptions.map((role) => (
                  <SelectItem key={role.roleId} value={getRoleLabel(role.code)}>
                    {getRoleLabel(role.code)}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            {errors.roleId && <p className="text-sm text-red-500 mt-1">{errors.roleId.message}</p>}
          </div>
        ) : (
          <div>
            <Label className="block text-base font-medium mb-1">Vai trò được gán tự động</Label>
            <div className="rounded-md border bg-muted/30 px-3 py-2 text-sm">
              {autoRoleLabel || "Chưa xác định vai trò cho loại tổ chức này"}
            </div>
            {errors.roleId && <p className="text-sm text-red-500 mt-1">{errors.roleId.message}</p>}
          </div>
        )}
      </div>

      <div className="flex justify-end">
        <Button type="submit" disabled={loading}>
          {loading ? "Đang tạo..." : "Tạo tài khoản"}
        </Button>
      </div>
    </form>
  );
}