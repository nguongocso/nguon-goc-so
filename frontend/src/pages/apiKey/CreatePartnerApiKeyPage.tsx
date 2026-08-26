import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";
import {
  KeyRound,
  ShieldCheck,
  AlertTriangle,
  Copy,
  Check,
  ArrowLeft,
  Info,
} from "lucide-react";
import { createApiKey } from "@/api/apiKeyApi";
import type { PartnerApiKeyResponse } from "@/types/apiKey";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { HelpButton } from "@/components/help/HelpButton";

const getDefaultExpiry = (): string => {
  const date = new Date();
  date.setMonth(date.getMonth() + 1);
  const pad = (n: number) => n.toString().padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
};

export const CreatePartnerApiKeyPage: React.FC = () => {
  const navigate = useNavigate();

  const [partnerName, setPartnerName] = useState("");
  const [rateLimitPerHour, setRateLimitPerHour] = useState<number | string>(100);
  const [expiresAt, setExpiresAt] = useState<string>(getDefaultExpiry());
  const [loading, setLoading] = useState(false);

  // Result state after creation
  const [createdKeyData, setCreatedKeyData] = useState<PartnerApiKeyResponse | null>(null);
  const [copied, setCopied] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!partnerName.trim()) {
      toast.error("Vui lòng nhập tên đối tác / doanh nghiệp thu mua");
      return;
    }

    const rateLimit = Number(rateLimitPerHour);
    if (isNaN(rateLimit) || rateLimit < 1) {
      toast.error("Hạn mức gọi API phải lớn hơn 0");
      return;
    }

    if (!expiresAt) {
      toast.error("Vui lòng chọn thời gian hết hạn của khóa");
      return;
    }

    const expiryDate = new Date(expiresAt);
    if (expiryDate.getTime() <= Date.now()) {
      toast.error("Thời gian hết hạn phải ở thời điểm tương lai");
      return;
    }

    setLoading(true);
    try {
      const response = await createApiKey({
        partnerName: partnerName.trim(),
        rateLimitPerHour: rateLimit,
        expiresAt: expiryDate.toISOString(),
      });

      toast.success("Cấp khóa API đối tác thành công!");
      setCreatedKeyData(response);
    } catch (err: any) {
      const errorMsg =
        err.response?.data?.message ||
        err.response?.data?.error ||
        "Có lỗi xảy ra khi cấp khóa API. Vui lòng thử lại.";
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
      toast.success("Đã sao chép khóa API vào khay nhớ tạm!");
      setTimeout(() => setCopied(false), 3000);
    } catch (err) {
      toast.error("Không thể sao chép tự động. Vui lòng chọn và sao chép thủ công.");
    }
  };

  return (
    <div className="space-y-6">
      {/* Header trang */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 flex items-center gap-2">
            <KeyRound className="size-6 text-emerald-600" />
            Cấp khóa API cho bên thứ ba
          </h1>
          <p className="text-sm text-muted-foreground mt-1">
            Tạo khóa truy cập cho phép phần mềm doanh nghiệp thu mua tự động lấy hồ sơ truy xuất lô sản xuất.
          </p>
        </div>
        <HelpButton screenKey="admin-api-keys" />
      </div>

      {createdKeyData ? (
        /* Result Screen after successful generation */
        <Card className="rounded-xl border-emerald-200 bg-white shadow-sm overflow-hidden">
          <CardHeader className="bg-emerald-50/60 border-b border-emerald-100 pb-4">
            <div className="flex items-center gap-2.5 text-emerald-800">
              <ShieldCheck className="h-6 w-6 text-emerald-600 shrink-0" />
              <div>
                <CardTitle className="text-lg font-bold text-emerald-950">
                  Khóa API đã được tạo thành công cho {createdKeyData.partnerName}!
                </CardTitle>
                <CardDescription className="text-xs text-emerald-700 mt-0.5">
                  Dưới đây là Khóa bản rõ dùng cho bên thứ ba tích hợp hệ thống.
                </CardDescription>
              </div>
            </div>
          </CardHeader>
          <CardContent className="p-6 space-y-5">
            {/* Warning Alert */}
            <div className="flex items-start gap-3 p-4 rounded-xl bg-amber-50 border border-amber-200 text-amber-900 text-sm">
              <AlertTriangle className="h-5 w-5 text-amber-600 shrink-0 mt-0.5" />
              <div>
                <p className="font-semibold text-amber-950">Lưu ý quan trọng về bảo mật:</p>
                <p className="mt-1 text-xs text-amber-800 leading-relaxed">
                  Hãy sao chép và lưu trữ chuỗi khóa API bí mật này ngay bây giờ. Vì lý do an toàn, bạn sẽ{" "}
                  <strong>không thể xem lại chuỗi khóa này một lần nào nữa</strong> sau khi rời khỏi trang.
                </p>
              </div>
            </div>

            {/* API Key Box */}
            <div className="space-y-2">
              <Label className="text-xs font-semibold text-slate-700 uppercase tracking-wider">
                Khóa API bí mật (Secret API Key)
              </Label>
              <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-2">
                <div className="relative flex-1">
                  <Input
                    readOnly
                    value={createdKeyData.rawApiKey || "Không thể hiển thị lại khóa"}
                    className="font-mono text-sm bg-slate-50 border-slate-300 select-all pr-10 text-slate-900 font-semibold"
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
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 p-4 bg-slate-50 rounded-xl border border-slate-100 text-xs">
              <div>
                <span className="text-muted-foreground block">Đối tác thụ hưởng:</span>
                <span className="font-semibold text-slate-900 text-sm mt-0.5 block">{createdKeyData.partnerName}</span>
              </div>
              <div>
                <span className="text-muted-foreground block">Hạn mức gọi:</span>
                <span className="font-semibold text-slate-900 text-sm mt-0.5 block">{createdKeyData.rateLimitPerHour} lượt / giờ</span>
              </div>
              <div>
                <span className="text-muted-foreground block">Thời hạn hiệu lực:</span>
                <span className="font-semibold text-slate-900 text-sm mt-0.5 block">
                  {new Date(createdKeyData.expiresAt).toLocaleString("vi-VN")}
                </span>
              </div>
            </div>

            <div className="flex justify-end pt-4 border-t border-slate-100">
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
        <Card className="rounded-xl border-slate-200 bg-white shadow-sm">
          <CardHeader className="border-b border-slate-100 pb-4">
            <CardTitle className="text-lg font-semibold text-slate-900">
              Thông tin cấu hình khóa API
            </CardTitle>
            <CardDescription>
              Thiết lập đối tác sử dụng, hạn mức lưu lượng và thời hạn hiệu lực cho khóa tích hợp.
            </CardDescription>
          </CardHeader>
          <form onSubmit={handleSubmit}>
            <CardContent className="space-y-5 pt-6">
              {/* Tên đối tác */}
              <div className="space-y-1.5">
                <Label htmlFor="partnerName" className="text-sm font-medium">
                  Tên đối tác / Doanh nghiệp thu mua <span className="text-red-500">*</span>
                </Label>
                <Input
                  id="partnerName"
                  placeholder="VD: Công ty TNHH Thu Mua Nông Sản ABC"
                  value={partnerName}
                  onChange={(e) => setPartnerName(e.target.value)}
                  disabled={loading}
                  required
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
                  max={100000}
                  placeholder="VD: 100"
                  value={rateLimitPerHour}
                  onChange={(e) => setRateLimitPerHour(e.target.value)}
                  disabled={loading}
                  required
                />
                <p className="text-xs text-muted-foreground">
                  Hệ thống sẽ chặn tự động nếu bên thứ ba gọi vượt quá số lượt này trong 1 giờ.
                </p>
              </div>

              {/* Thời gian hết hạn khóa */}
              <div className="space-y-1.5">
                <Label htmlFor="expiresAt" className="text-sm font-medium">
                  Thời gian hết hạn khóa <span className="text-red-500">*</span>
                </Label>
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
                  Thời gian hết hạn bắt buộc phải ở thời điểm tương lai.
                </p>
              </div>

              {/* Cảnh báo ghi chú */}
              <div className="flex items-start gap-2.5 p-3.5 rounded-xl bg-blue-50 border border-blue-200 text-blue-900 text-xs leading-relaxed">
                <Info className="h-4 w-4 text-blue-600 shrink-0 mt-0.5" />
                <span>
                  Sau khi bấm cấp khóa, hệ thống sẽ sinh ra chuỗi API Key bản rõ <strong>1 lần duy nhất</strong>. Bạn cần sao chép ngay để gửi cho phía đối tác tích hợp phần mềm.
                </span>
              </div>

              <div className="flex justify-end gap-3 pt-4 border-t border-slate-100">
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => navigate("/integration/api-keys")}
                  disabled={loading}
                >
                  Hủy
                </Button>
                <Button type="submit" variant="create" disabled={loading}>
                  <KeyRound className="h-4 w-4 mr-1.5" />
                  {loading ? "Đang xử lý..." : "Cấp khóa mới"}
                </Button>
              </div>
            </CardContent>
          </form>
        </Card>
      )}
    </div>
  );
};

export default CreatePartnerApiKeyPage;
