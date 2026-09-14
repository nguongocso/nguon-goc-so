import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";
import {
  FlaskConical,
  ShieldCheck,
  AlertTriangle,
  Copy,
  Check,
  ArrowLeft,
  Info,
} from "lucide-react";
import { createTestApiKey } from "@/api/apiKeyApi";
import type { PartnerApiKeyResponse } from "@/types/apiKey";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { HelpButton } from "@/components/help/HelpButton";
import { useSetBreadcrumb } from "@/components/common/AppBreadcrumb";

const getDefaultExpiry = (days = 14): string => {
  const date = new Date();
  date.setDate(date.getDate() + days);
  const pad = (n: number) => n.toString().padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
};

export const CreateTestPartnerApiKeyPage: React.FC = () => {
  const navigate = useNavigate();

  useSetBreadcrumb([
    { label: "Tổng quan", href: "/dashboard" },
    { label: "Khóa API đối tác", href: "/integration/api-keys" },
    { label: "Cấp khóa thử nghiệm" },
  ]);

  const [partnerName, setPartnerName] = useState("");
  const [rateLimitPerHour, setRateLimitPerHour] = useState<number | string>(100);
  const [expiresAt, setExpiresAt] = useState<string>(getDefaultExpiry(14));
  const [loading, setLoading] = useState(false);

  // Result state after creation
  const [createdKeyData, setCreatedKeyData] = useState<PartnerApiKeyResponse | null>(null);
  const [copied, setCopied] = useState(false);

  const handleSetQuickDays = (days: number) => {
    setExpiresAt(getDefaultExpiry(days));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!partnerName.trim()) {
      toast.error("Vui lòng nhập tên đối tác / đơn vị thử nghiệm");
      return;
    }

    const rateLimit = Number(rateLimitPerHour);
    if (isNaN(rateLimit) || rateLimit < 1) {
      toast.error("Hạn mức gọi API phải lớn hơn 0");
      return;
    }
    if (rateLimit > 100) {
      toast.error("Hạn mức thử nghiệm tối đa là 100 lượt/giờ theo quy định");
      return;
    }

    if (!expiresAt) {
      toast.error("Vui lòng chọn thời gian hết hạn của khóa");
      return;
    }

    const expiryDate = new Date(expiresAt);
    const now = new Date();
    if (expiryDate.getTime() <= now.getTime()) {
      toast.error("Thời gian hết hạn phải ở thời điểm tương lai");
      return;
    }

    // Kiểm tra tối đa 30 ngày
    const maxDate = new Date(now.getTime() + 30 * 24 * 60 * 60 * 1000 + 3600000); // 30 ngày + 1h buffer
    if (expiryDate.getTime() > maxDate.getTime()) {
      toast.error("Thời hạn thử nghiệm tối đa là 30 ngày theo quy định bảo mật");
      return;
    }

    setLoading(true);
    try {
      const response = await createTestApiKey({
        partnerName: partnerName.trim(),
        rateLimitPerHour: rateLimit,
        expiresAt: expiryDate.toISOString(),
      });

      toast.success("Cấp khóa API thử nghiệm thành công!");
      setCreatedKeyData(response);
    } catch (err: any) {
      const errorMsg =
        err.response?.data?.message ||
        err.response?.data?.error ||
        "Có lỗi xảy ra khi cấp khóa thử nghiệm. Vui lòng thử lại.";
      toast.error(errorMsg);
    } finally {
      setLoading(false);
    }
  };

  const handleCopyKey = async () => {
    if (!createdKeyData?.rawApiKey) return;
    try {
      await navigator.clipboard.writeText(createdKeyData.rawApiKey);
      setCopied(true);
      toast.success("Đã sao chép khóa API thử nghiệm vào khay nhớ tạm!");
      setTimeout(() => setCopied(false), 3000);
    } catch {
      toast.error("Không thể sao chép tự động. Vui lòng chọn và sao chép thủ công.");
    }
  };

  return (
    <div className="space-y-6">
      {/* Header trang */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-slate-100 flex items-center gap-2">
            <FlaskConical className="size-6 text-emerald-600 dark:text-emerald-500" />
            Cấp khóa API thử nghiệm
          </h1>
          <p className="text-sm text-muted-foreground mt-1">
            Tạo khóa truy cập Sandbox cho phép đội kỹ thuật bên thứ ba kết nối và kiểm thử API với dữ liệu mẫu chuẩn hóa, an toàn tuyệt đối.
          </p>
        </div>
        <HelpButton screenKey="admin-api-keys" />
      </div>

      {createdKeyData ? (
        /* Result Screen after successful generation */
        <Card className="rounded-xl border-emerald-200 dark:border-emerald-800 bg-white dark:bg-card shadow-sm overflow-hidden">
          <CardHeader className="bg-emerald-50/60 dark:bg-emerald-950/40 border-b border-emerald-100 dark:border-emerald-800 pb-4">
            <div className="flex items-center gap-2.5 text-emerald-800 dark:text-emerald-200">
              <ShieldCheck className="h-6 w-6 text-emerald-600 dark:text-emerald-400 shrink-0" />
              <div>
                <CardTitle className="text-lg font-bold text-emerald-950 dark:text-emerald-100">
                  Khóa API thử nghiệm đã được tạo thành công cho {createdKeyData.partnerName}!
                </CardTitle>
                <CardDescription className="text-xs text-emerald-700 dark:text-emerald-300 mt-0.5">
                  Dưới đây là Khóa bản rõ Sandbox dùng cho bên thứ ba tích hợp và kiểm thử hệ thống.
                </CardDescription>
              </div>
            </div>
          </CardHeader>
          <CardContent className="p-6 space-y-5">
            {/* Warning Alert */}
            <div className="flex items-start gap-3 p-4 rounded-xl bg-amber-50 dark:bg-amber-950/40 border border-amber-200 dark:border-amber-800 text-amber-900 dark:text-amber-200 text-sm">
              <AlertTriangle className="h-5 w-5 text-amber-600 dark:text-amber-400 shrink-0 mt-0.5" />
              <div>
                <p className="font-semibold text-amber-950 dark:text-amber-100">Lưu ý quan trọng về bảo mật:</p>
                <p className="mt-1 text-xs text-amber-800 dark:text-amber-300 leading-relaxed">
                  Hãy sao chép và lưu trữ chuỗi khóa API bí mật này ngay bây giờ. Vì lý do an toàn, bạn sẽ{" "}
                  <strong>không thể xem lại chuỗi khóa này một lần nào nữa</strong> sau khi rời khỏi trang.
                </p>
              </div>
            </div>

            {/* API Key Box */}
            <div className="space-y-2">
              <Label className="text-xs font-semibold text-slate-700 dark:text-slate-300 uppercase tracking-wider">
                Khóa API thử nghiệm (Header X-API-KEY)
              </Label>
              <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-2">
                <div className="relative flex-1">
                  <Input
                    readOnly
                    value={createdKeyData.rawApiKey || "Không thể hiển thị lại khóa"}
                    className="font-mono text-sm bg-slate-50 dark:bg-slate-900 border-slate-300 dark:border-slate-700 select-all pr-10 text-emerald-600 dark:text-emerald-400 font-semibold"
                    onClick={(e) => (e.target as HTMLInputElement).select()}
                  />
                </div>
                <Button
                  type="button"
                  onClick={handleCopyKey}
                  variant={copied ? "create" : "outline"}
                  className="shrink-0 gap-1.5"
                >
                  {copied ? (
                    <>
                      <Check className="h-4 w-4 text-white" />
                      <span>Đã sao chép</span>
                    </>
                  ) : (
                    <>
                      <Copy className="h-4 w-4" />
                      <span>Sao chép khóa</span>
                    </>
                  )}
                </Button>
              </div>
            </div>

            {/* Thông tin cấu hình tóm tắt */}
            <div className="grid grid-cols-1 sm:grid-cols-4 gap-3 p-4 bg-slate-50 dark:bg-slate-900/50 rounded-xl border border-slate-100 dark:border-slate-800 text-xs">
              <div>
                <span className="text-muted-foreground block">Đối tác thụ hưởng:</span>
                <span className="font-semibold text-slate-900 dark:text-slate-100 text-sm mt-0.5 block">{createdKeyData.partnerName}</span>
              </div>
              <div>
                <span className="text-muted-foreground block">Môi trường:</span>
                <span className="font-semibold text-blue-600 dark:text-blue-400 text-sm mt-0.5 block">Sandbox (is_test=true)</span>
              </div>
              <div>
                <span className="text-muted-foreground block">Hạn mức gọi:</span>
                <span className="font-semibold text-slate-900 dark:text-slate-100 text-sm mt-0.5 block">{createdKeyData.rateLimitPerHour} lượt / giờ</span>
              </div>
              <div>
                <span className="text-muted-foreground block">Thời hạn hiệu lực:</span>
                <span className="font-semibold text-slate-900 dark:text-slate-100 text-sm mt-0.5 block">
                  {new Date(createdKeyData.expiresAt).toLocaleString("vi-VN")}
                </span>
              </div>
            </div>

            <div className="flex justify-end pt-4 border-t border-slate-100 dark:border-slate-800">
              <Button
                type="button"
                variant="create"
                onClick={() => navigate("/integration/api-keys")}
              >
                <ArrowLeft className="h-4 w-4 mr-1.5" />
                Hoàn tất & Quay lại danh sách
              </Button>
            </div>
          </CardContent>
        </Card>
      ) : (
        /* Create Form Card */
        <Card className="rounded-xl border-slate-200 dark:border-slate-800 bg-white dark:bg-card shadow-sm">
          <CardHeader className="border-b border-slate-100 dark:border-slate-800 pb-4">
            <CardTitle className="text-lg font-semibold text-slate-900 dark:text-slate-100">
              Thông tin cấu hình khóa thử nghiệm
            </CardTitle>
            <CardDescription>
              Thiết lập đối tác sử dụng, hạn mức thử nghiệm (tối đa 100 lượt/giờ) và thời hạn hiệu lực (tối đa 30 ngày).
            </CardDescription>
          </CardHeader>
          <form noValidate onSubmit={handleSubmit}>
            <CardContent className="space-y-5 pt-6">
              {/* Tên đối tác */}
              <div className="space-y-1.5">
                <Label htmlFor="partnerName" className="text-sm font-medium">
                  Tên đối tác / Đơn vị thử nghiệm <span className="text-red-500">*</span>
                </Label>
                <Input
                  id="partnerName"
                  placeholder="VD: Đội kỹ thuật Big C / Phần mềm ERP đối tác"
                  value={partnerName}
                  onChange={(e) => setPartnerName(e.target.value)}
                  disabled={loading}
                  required
                  autoFocus
                />
              </div>

              {/* Hạn mức số lượt gọi / giờ */}
              <div className="space-y-1.5">
                <Label htmlFor="rateLimitPerHour" className="text-sm font-medium">
                  Hạn mức gọi API (Số lượt / giờ) <span className="text-red-500">*</span>
                </Label>
                <Input
                  id="rateLimitPerHour"
                  type="number"
                  min={1}
                  max={100}
                  placeholder="VD: 100"
                  value={rateLimitPerHour}
                  onChange={(e) => setRateLimitPerHour(e.target.value)}
                  disabled={loading}
                  required
                />
                <p className="text-xs text-muted-foreground">
                  Khóa thử nghiệm giới hạn tối đa 100 lượt/giờ theo quy định bảo mật tài nguyên.
                </p>
              </div>

              {/* Thời gian hết hạn khóa */}
              <div className="space-y-1.5">
                <div className="flex items-center justify-between">
                  <Label htmlFor="expiresAt" className="text-sm font-medium">
                    Thời gian hết hạn khóa <span className="text-red-500">*</span>
                  </Label>
                  <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
                    <span>Chọn nhanh:</span>
                    {[7, 14, 30].map((days) => (
                      <button
                        key={days}
                        type="button"
                        onClick={() => handleSetQuickDays(days)}
                        className="px-2 py-0.5 rounded border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 hover:bg-slate-100 dark:hover:bg-slate-700 text-[11px] font-medium transition-colors"
                      >
                        +{days} ngày
                      </button>
                    ))}
                  </div>
                </div>
                <Input
                  id="expiresAt"
                  type="datetime-local"
                  value={expiresAt}
                  onChange={(e) => setExpiresAt(e.target.value)}
                  disabled={loading}
                  required
                  className="max-w-md"
                />
                <p className="text-xs text-muted-foreground">
                  Thời hạn thử nghiệm tối đa là 30 ngày kể từ thời điểm tạo.
                </p>
              </div>

              {/* Cảnh báo ghi chú */}
              <div className="flex items-start gap-2.5 p-3.5 rounded-xl bg-blue-50 dark:bg-blue-950/40 border border-blue-200 dark:border-blue-800 text-blue-900 dark:text-blue-200 text-xs leading-relaxed">
                <Info className="h-4 w-4 text-blue-600 dark:text-blue-400 shrink-0 mt-0.5" />
                <span>
                  Sau khi bấm cấp khóa, hệ thống sẽ sinh ra chuỗi API Key bản rõ <strong>1 lần duy nhất</strong>. Bạn cần sao chép ngay để gửi cho phía đối tác tích hợp phần mềm.
                </span>
              </div>

              <div className="flex justify-end gap-3 pt-4 border-t border-slate-100 dark:border-slate-800">
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => navigate("/integration/api-keys")}
                  disabled={loading}
                >
                  Hủy
                </Button>
                <Button type="submit" variant="create" disabled={loading} className="gap-1.5">
                  <FlaskConical className="h-4 w-4" />
                  {loading ? "Đang xử lý..." : "Cấp khóa thử nghiệm"}
                </Button>
              </div>
            </CardContent>
          </form>
        </Card>
      )}
    </div>
  );
};

export default CreateTestPartnerApiKeyPage;
