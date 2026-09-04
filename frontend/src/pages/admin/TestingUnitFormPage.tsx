import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { toast } from "sonner";
import { BadgeCheck, Loader2, Save, ShieldCheck } from "lucide-react";

import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { useSetBreadcrumb } from "@/components/common/AppBreadcrumb";
import { HelpButton } from "@/components/help/HelpButton";

import {
  createTestingUnit,
  getTestingUnits,
  updateTestingUnit,
} from "@/api/certificationApi";

/** Ngày hiện tại dạng YYYY-MM-DD (giờ địa phương). */
const toISODate = (date: Date) => {
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${date.getFullYear()}-${month}-${day}`;
};

interface FormState {
  name: string;
  accreditationCode: string;
  contactInfo: string;
  accreditationExpiryDate: string;
}

const emptyForm: FormState = {
  name: "",
  accreditationCode: "",
  contactInfo: "",
  accreditationExpiryDate: "",
};

/**
 * Trang tạo / chỉnh sửa đơn vị kiểm nghiệm (VT-01).
 * Route: /admin/testing-units/create | /admin/testing-units/:id/edit
 */
export default function TestingUnitFormPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const isEdit = !!id;

  const [form, setForm] = useState<FormState>(emptyForm);
  const [loading, setLoading] = useState(isEdit);
  const [saving, setSaving] = useState(false);
  const [errors, setErrors] = useState<Partial<Record<keyof FormState, string>>>({});

  // Tải thông tin đơn vị khi ở chế độ chỉnh sửa.
  useEffect(() => {
    if (!id) return;
    let cancelled = false;

    const loadUnit = async () => {
      setLoading(true);
      try {
        const data = await getTestingUnits({ page: 0, size: 500 });
        if (cancelled) return;
        const found = data.items.find((u) => u.id === id) ?? null;
        if (!found) {
          toast.error("Không tìm thấy đơn vị kiểm nghiệm.");
          navigate("/admin/testing-units");
          return;
        }
        setForm({
          name: found.name,
          accreditationCode: found.accreditationCode,
          contactInfo: found.contactInfo ?? "",
          accreditationExpiryDate: found.accreditationExpiryDate ?? "",
        });
      } catch (error: any) {
        if (!cancelled) {
          toast.error(
            error.response?.data?.message ||
              "Không thể tải thông tin đơn vị kiểm nghiệm"
          );
          navigate("/admin/testing-units");
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    };

    void loadUnit();

    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  const today = toISODate(new Date());

  const validate = (): boolean => {
    const next: Partial<Record<keyof FormState, string>> = {};

    if (!form.name.trim()) {
      next.name = "Tên đơn vị kiểm nghiệm không được để trống.";
    } else if (form.name.trim().length > 255) {
      next.name = "Tên đơn vị kiểm nghiệm tối đa 255 ký tự.";
    }

    if (!form.accreditationCode.trim()) {
      next.accreditationCode = "Mã công nhận không được để trống.";
    } else if (form.accreditationCode.trim().length > 100) {
      next.accreditationCode = "Mã công nhận tối đa 100 ký tự.";
    }

    if (form.contactInfo.trim().length > 500) {
      next.contactInfo = "Thông tin liên hệ tối đa 500 ký tự.";
    }

    if (
      form.accreditationExpiryDate &&
      form.accreditationExpiryDate < today
    ) {
      next.accreditationExpiryDate =
        "Ngày hết hạn công nhận không được ở quá khứ.";
    }

    setErrors(next);
    return Object.keys(next).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (saving) return;

    if (!validate()) return;

    setSaving(true);
    try {
      const payload = {
        name: form.name.trim(),
        accreditationCode: form.accreditationCode.trim(),
        contactInfo: form.contactInfo.trim() || null,
        accreditationExpiryDate:
          form.accreditationExpiryDate || null,
      };

      if (isEdit && id) {
        await updateTestingUnit(id, payload);
        toast.success(`Đã cập nhật đơn vị "${payload.name}"`);
      } else {
        await createTestingUnit(payload);
        toast.success(`Đã tạo đơn vị "${payload.name}"`);
      }
      navigate("/admin/testing-units");
    } catch (error: any) {
      toast.error(
        error.response?.data?.message ||
          (isEdit
            ? "Không thể cập nhật đơn vị kiểm nghiệm"
            : "Không thể tạo đơn vị kiểm nghiệm")
      );
    } finally {
      setSaving(false);
    }
  };

  useSetBreadcrumb(
    isEdit
      ? [
          { label: "Tổng quan", href: "/dashboard" },
          { label: "Đơn vị kiểm nghiệm", href: "/admin/testing-units" },
          { label: "Chỉnh sửa" },
        ]
      : [
          { label: "Tổng quan", href: "/dashboard" },
          { label: "Đơn vị kiểm nghiệm", href: "/admin/testing-units" },
          { label: "Tạo mới" },
        ]
  );

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20 text-muted-foreground">
        <Loader2 className="h-6 w-6 animate-spin mr-2 text-emerald-600" />
        Đang tải thông tin đơn vị kiểm nghiệm...
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 flex items-center gap-2">
            <ShieldCheck className="size-6 text-emerald-600" />
            {isEdit ? "Chỉnh sửa đơn vị kiểm nghiệm" : "Tạo đơn vị kiểm nghiệm"}
          </h1>
          <p className="text-sm text-muted-foreground mt-1">
            Khai báo thông tin phòng thí nghiệm / đơn vị kiểm nghiệm trong
            danh mục dùng chung.
          </p>
        </div>
        <HelpButton screenKey="testing-unit-form" />
      </div>

      <Card className="rounded-xl border-slate-200 bg-white shadow-sm">
        <CardHeader className="border-b border-slate-100 pb-4">
          <CardTitle className="text-lg font-semibold text-slate-900">
            Thông tin đơn vị kiểm nghiệm
          </CardTitle>
          <CardDescription>
            Tên đơn vị phải là duy nhất. Mã công nhận là mã trình độ kỹ thuật
            của đơn vị (ví dụ VILAS).
          </CardDescription>
        </CardHeader>
        <CardContent className="pt-6">
          <form onSubmit={handleSubmit} className="space-y-5">
            <div className="space-y-1.5">
              <Label htmlFor="name" className="text-xs font-semibold text-foreground">
                Tên đơn vị kiểm nghiệm <span className="text-red-600">*</span>
              </Label>
              <Input
                id="name"
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
                placeholder="Ví dụ: Trung tâm Kỹ thuật Tiêu chuẩn Đo lường Chất lượng 3"
                className="h-10 rounded-xl text-xs"
                maxLength={255}
              />
              {errors.name && (
                <p className="text-xs font-medium text-red-600">{errors.name}</p>
              )}
            </div>

            <div className="space-y-1.5">
              <Label
                htmlFor="accreditationCode"
                className="text-xs font-semibold text-foreground"
              >
                Mã công nhận <span className="text-red-600">*</span>
              </Label>
              <div className="relative">
                <BadgeCheck className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-emerald-600" />
                <Input
                  id="accreditationCode"
                  value={form.accreditationCode}
                  onChange={(e) =>
                    setForm({ ...form, accreditationCode: e.target.value })
                  }
                  placeholder="Ví dụ: VILAS 1234"
                  className="h-10 rounded-xl pl-9 text-xs font-mono"
                  maxLength={100}
                />
              </div>
              {errors.accreditationCode && (
                <p className="text-xs font-medium text-red-600">
                  {errors.accreditationCode}
                </p>
              )}
            </div>


            <div className="space-y-1.5">
              <Label
                htmlFor="contactInfo"
                className="text-xs font-semibold text-foreground"
              >
                Thông tin liên hệ
              </Label>
              <Textarea
                id="contactInfo"
                rows={3}
                value={form.contactInfo}
                onChange={(e) =>
                  setForm({ ...form, contactInfo: e.target.value })
                }
                placeholder="Địa chỉ, điện thoại, email của đơn vị kiểm nghiệm..."
                className="rounded-xl text-xs border-input resize-none"
                maxLength={500}
              />
              {errors.contactInfo && (
                <p className="text-xs font-medium text-red-600">
                  {errors.contactInfo}
                </p>
              )}
            </div>

            <div className="space-y-1.5">
              <Label
                htmlFor="accreditationExpiryDate"
                className="text-xs font-semibold text-foreground"
              >
                Ngày hết hạn công nhận
              </Label>
              <Input
                id="accreditationExpiryDate"
                type="date"
                min={today}
                value={form.accreditationExpiryDate}
                onChange={(e) =>
                  setForm({
                    ...form,
                    accreditationExpiryDate: e.target.value,
                  })
                }
                className="h-10 rounded-xl text-xs border-input max-w-sm"
              />
              <p className="text-xs text-muted-foreground">
                Để trống nếu đơn vị không có ngày hết hạn công nhận. Đơn vị hết
                hạn sẽ không được phép nhận yêu cầu kiểm nghiệm.
              </p>
              {errors.accreditationExpiryDate && (
                <p className="text-xs font-medium text-red-600">
                  {errors.accreditationExpiryDate}
                </p>
              )}
            </div>

            {/* Footer actions */}
            <div className="flex items-center justify-end border-t pt-4">
              <Button type="submit" disabled={saving}>
                {saving ? (
                  <Loader2 className="h-4 w-4 mr-1 animate-spin" />
                ) : (
                  <Save className="h-4 w-4 mr-1" />
                )}
                {saving
                  ? "Đang lưu..."
                  : isEdit
                    ? "Cập nhật đơn vị"
                    : "Tạo đơn vị"}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}

