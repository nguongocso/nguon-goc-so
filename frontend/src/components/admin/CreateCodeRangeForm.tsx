import { createCodeRange } from "@/api/codeRangeApi";
import { getOrganizations } from "@/api/organizationApi";
import { useAuth } from "@/hooks/useAuth";
import { type Organization } from "@/types/organization";
import {
  type CreateCodeRangeFormValues,
  createCodeRangeSchema,
} from "@/utils/validators";
import { selectAllOnFocus, preventMouseUpCollapse } from "@/utils/inputUtils";
import { zodResolver } from "@hookform/resolvers/zod";
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "../ui/card";
import { Label } from "../ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "../ui/select";
import { Input } from "../ui/input";
import { Button } from "../ui/button";

export const CreateCodeRangeForm: React.FC = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [organizations, setOrganizations] = useState<Organization[]>([]);

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    formState: { errors },
  } = useForm<CreateCodeRangeFormValues>({
    resolver: zodResolver(createCodeRangeSchema),
    defaultValues: {
      organizationId: "",
      prefix: "",
      totalLimit: undefined,
    },
  });

  const selectedOrgId = watch("organizationId");

  const isAdmin = user?.roleCode === "VT-01";

  useEffect(() => {
    const fetchOrganizations = async () => {
      try {
        const data = await getOrganizations();

        const mappedData: Organization[] = data.map((item: any) => ({
          id: item.organizationID,
          name: item.organizationName,
          code: item.organizationCode,
          type: item.organizationType,
          status: item.status,
          createdAt: item.createdAt,
        }));
        setOrganizations(mappedData);
      } catch (error) {
        toast.error("Không thể tải danh sách tổ chức");
      } finally {
        setLoading(false);
      }
    };
    if (isAdmin) {
      fetchOrganizations();
    } else {
      setLoading(false);
    }
  }, [isAdmin]);

  const onSubmit = async (data: CreateCodeRangeFormValues) => {
    setSubmitting(true);
    try {
      const result = await createCodeRange({
        organizationId: data.organizationId,
        prefix: data.prefix,
        totalLimit: data.totalLimit,
      });
      toast.success(`Cấp dải mã thành công: ${result.prefix}`);
      navigate("/admin/code-ranges");
    } catch (error: any) {
      const message = error.response?.data?.message || "Cấp dải mã thất bại";
      toast.error(message);
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return <div className="flex justify-center p-8">Đang tải...</div>;
  }

  if (!isAdmin) {
    return (
      <div className="text-center p-8 text-red-500">
        Bạn không có quyền truy cập trang này.
      </div>
    );
  }

  return (
    <Card className="rounded-xl border-slate-200 bg-white shadow-sm">
      <CardHeader className="border-b border-slate-100 pb-4">
        <CardTitle className="text-lg font-semibold text-slate-900">
          Thông tin cấp dải mã
        </CardTitle>
        <CardDescription>
          Chọn tổ chức thụ hưởng, tiền tố mã định danh và hạn mức số lượng tem tối đa.
        </CardDescription>
      </CardHeader>
      <form onSubmit={handleSubmit(onSubmit)}>
        <CardContent className="space-y-4 pt-6">
          {/* Tổ chức */}
          <div className="space-y-2">
            <Label htmlFor="organizationId">Tổ chức *</Label>
            <Select
              value={selectedOrgId || ""}
              onValueChange={(val) => {
                if (val !== null && val !== undefined) {
                  setValue("organizationId", val, { shouldValidate: true });
                }
              }}
            >
              <SelectTrigger className="w-full">
                <SelectValue placeholder="Chọn tổ chức">
                  {selectedOrgId
                    ? organizations.find((org) => org.id === selectedOrgId)?.name
                    : undefined}
                </SelectValue>
              </SelectTrigger>
              <SelectContent>
                {organizations.length === 0 ? (
                  <div className="p-2 text-sm text-gray-500">Không có tổ chức nào</div>
                ) : (
                  organizations.map((org) => (
                    <SelectItem key={org.id} value={org.id}>
                      {org.name} ({org.code})
                    </SelectItem>
                  ))
                )}
              </SelectContent>
            </Select>
            {errors.organizationId && (
              <p className="text-sm text-red-500">
                {errors.organizationId.message}
              </p>
            )}
          </div>

          {/* Tiền tố mã */}
          <div className="space-y-2">
            <Label htmlFor="prefix">Tiền tố mã *</Label>
            <Input
              id="prefix"
              {...register("prefix")}
              placeholder="893001"
            />
            <p className="text-sm text-gray-500">
              Duy nhất trong hệ thống, chỉ gồm chữ hoa (A–Z) và chữ số, tối đa
              50 ký tự. Ví dụ: 893001.
            </p>
            {errors.prefix && (
              <p className="text-sm text-red-500">{errors.prefix.message}</p>
            )}
          </div>

          {/* Hạn mức */}
          <div className="space-y-2">
            <Label htmlFor="totalLimit">Hạn mức (số tem tối đa) *</Label>
            <Input
              id="totalLimit"
              type="number"
              step="1"
              {...register("totalLimit")}
              onFocus={selectAllOnFocus}
              onMouseUp={preventMouseUpCollapse}
              placeholder="Nhập số lượng tem tối đa"
            />
            <p className="text-sm text-gray-500">
              Nhập số lượng tem tối đa (số nguyên dương).
            </p>
            {errors.totalLimit && (
              <p className="text-sm text-red-500">
                {errors.totalLimit.message}
              </p>
            )}
          </div>
        </CardContent>
        <CardFooter className="flex justify-end gap-2">
          <Button
            type="button"
            variant="outline"
            onClick={() => navigate("/admin/code-ranges")}
          >
            Hủy
          </Button>
          <Button type="submit" disabled={submitting}>
            {submitting ? "Đang xử lý..." : "Cấp dải mã"}
          </Button>
        </CardFooter>
      </form>
    </Card>
  );
};