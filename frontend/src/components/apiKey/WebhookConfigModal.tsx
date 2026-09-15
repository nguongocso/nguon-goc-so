import React, { useState, useEffect } from 'react';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Switch } from '@/components/ui/switch';
import { Badge } from '@/components/ui/badge';
import { toast } from 'sonner';
import {
  Webhook,
  Send,
  Copy,
  Check,
  Eye,
  EyeOff,
  AlertCircle,
  CheckCircle2,
  XCircle,
  Loader2,
  ShieldCheck,
} from 'lucide-react';
import type { PartnerApiKeyResponse, WebhookTestPingResponse } from '@/types/apiKey';
import { updatePartnerWebhook, testPingPartnerWebhook } from '@/api/apiKeyApi';

interface WebhookConfigModalProps {
  open: boolean;
  apiKey: PartnerApiKeyResponse | null;
  onClose: () => void;
  onSuccess: () => void;
}

export const WebhookConfigModal: React.FC<WebhookConfigModalProps> = ({
  open,
  apiKey,
  onClose,
  onSuccess,
}) => {
  const [webhookUrl, setWebhookUrl] = useState('');
  const [isActive, setIsActive] = useState(true);
  const [showSecret, setShowSecret] = useState(false);
  const [copiedSecret, setCopiedSecret] = useState(false);
  const [saving, setSaving] = useState(false);
  const [pinging, setPinging] = useState(false);
  const [pingResult, setPingResult] = useState<WebhookTestPingResponse | null>(null);
  const [urlError, setUrlError] = useState<string | null>(null);

  useEffect(() => {
    if (apiKey) {
      setWebhookUrl(apiKey.webhookUrl || '');
      setIsActive(apiKey.isWebhookActive !== false);
      setPingResult(null);
      setUrlError(null);
      setShowSecret(false);
    }
  }, [apiKey, open]);

  if (!apiKey) {
    return null;
  }

  const validateUrl = (url: string): boolean => {
    if (!url.trim()) {
      setUrlError(null);
      return true; // Cho phép để trống để hủy webhook
    }

    try {
      const parsed = new URL(url.trim());
      const isHttps = parsed.protocol === 'https:';
      const isLocalhost = parsed.protocol === 'http:' && (parsed.hostname === 'localhost' || parsed.hostname === '127.0.0.1');

      if (!isHttps && !isLocalhost) {
        setUrlError('Địa chỉ Webhook bắt buộc phải sử dụng giao thức bảo mật HTTPS (https://).');
        return false;
      }

      setUrlError(null);
      return true;
    } catch {
      setUrlError('Định dạng URL không hợp lệ (ví dụ: https://partner.example.com/webhooks).');
      return false;
    }
  };

  const handleUrlChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const val = e.target.value;
    setWebhookUrl(val);
    if (val.trim()) {
      validateUrl(val);
    } else {
      setUrlError(null);
    }
  };

  const handleSave = async () => {
    if (!validateUrl(webhookUrl)) {
      return;
    }

    setSaving(true);
    try {
      await updatePartnerWebhook(apiKey.id, {
        webhookUrl: webhookUrl.trim(),
        isActive,
      });

      toast.success('Cập nhật cấu hình Webhook thành công!');
      onSuccess();
      onClose();
    } catch (err: any) {
      const msg = err.response?.data?.message || 'Không thể lưu cấu hình Webhook. Vui lòng thử lại.';
      toast.error(msg);
    } finally {
      setSaving(false);
    }
  };

  const handleTestPing = async () => {
    if (!apiKey.webhookUrl && !webhookUrl.trim()) {
      toast.error('Vui lòng nhập và lưu địa chỉ Webhook trước khi bắn thử nghiệm.');
      return;
    }

    // Nếu người dùng vừa thay đổi URL chưa lưu, yêu cầu lưu trước
    if (webhookUrl.trim() !== (apiKey.webhookUrl || '').trim()) {
      toast.warning('Bạn đã thay đổi URL. Vui lòng nhấn "Lưu cấu hình" trước khi bắn thử.');
      return;
    }

    setPinging(true);
    setPingResult(null);
    try {
      const result = await testPingPartnerWebhook(apiKey.id);
      setPingResult(result);
      if (result.isSuccess) {
        toast.success(`Bắn thử webhook thành công! (HTTP ${result.httpStatus}, ${result.durationMs}ms)`);
      } else {
        toast.error(`Máy chủ đối tác phản hồi lỗi hoặc không kết nối được.`);
      }
    } catch (err: any) {
      const msg = err.response?.data?.message || 'Lỗi khi gửi yêu cầu bắn thử webhook.';
      toast.error(msg);
    } finally {
      setPinging(false);
    }
  };

  const handleCopySecret = async () => {
    if (!apiKey.webhookSecret) return;
    try {
      await navigator.clipboard.writeText(apiKey.webhookSecret);
      setCopiedSecret(true);
      toast.success('Đã sao chép khóa bí mật Webhook!');
      setTimeout(() => setCopiedSecret(false), 3000);
    } catch {
      toast.error('Không thể sao chép tự động.');
    }
  };

  return (
    <Dialog open={open} onOpenChange={(isOpen) => !isOpen && onClose()}>
      <DialogContent className="sm:max-w-xl">
        <DialogHeader>
          <div className="flex items-center gap-2 text-primary font-semibold mb-1">
            <Webhook className="w-5 h-5 text-primary" />
            <span>Cấu hình Webhook thông báo thu hồi (NCL-12-CN-006)</span>
          </div>
          <DialogTitle className="text-xl">
            Đối tác: {apiKey.partnerName}
          </DialogTitle>
          <DialogDescription className="text-muted-foreground pt-1">
            Hệ thống tự động bắn gói tin HTTP POST thông báo tới địa chỉ này ngay khi lô hàng đối tác đã truy xuất bị thu hồi.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4 py-3">
          {/* Nhập URL */}
          <div className="space-y-2">
            <Label htmlFor="webhook-url" className="text-sm font-medium flex items-center justify-between">
              <span>Địa chỉ URL Webhook (Endpoint HTTPS) <span className="text-destructive">*</span></span>
              {apiKey.webhookUrl && (
                <span className="text-xs text-muted-foreground font-normal">
                  (Xóa trống để hủy đăng ký)
                </span>
              )}
            </Label>
            <div className="relative">
              <Input
                id="webhook-url"
                placeholder="https://partner.com/api/v1/webhook-listener"
                value={webhookUrl}
                onChange={handleUrlChange}
                className={`font-mono text-xs sm:text-sm ${urlError ? 'border-destructive focus-visible:ring-destructive' : ''}`}
              />
            </div>
            {urlError && (
              <p className="text-xs text-destructive flex items-center gap-1.5 mt-1">
                <AlertCircle className="w-3.5 h-3.5 shrink-0" />
                <span>{urlError}</span>
              </p>
            )}
            <p className="text-[12px] text-muted-foreground">
              Địa chỉ nhận tin phải mở cổng HTTPS công khai và phản hồi mã trạng thái 2xx trong vòng 5 giây.
            </p>
          </div>

          {/* Toggle Kích hoạt nhận tin */}
          <div className="flex items-center justify-between p-3 rounded-lg border bg-muted/30">
            <div className="space-y-0.5">
              <Label className="text-sm font-medium">Bật nhận thông báo tự động</Label>
              <p className="text-xs text-muted-foreground">
                Tạm dừng sẽ không gửi hoặc xếp hàng thông báo khi lô hàng bị thu hồi.
              </p>
            </div>
            <Switch
              checked={isActive}
              onCheckedChange={setIsActive}
            />
          </div>

          {/* Khóa bí mật chữ ký số HMAC-SHA256 */}
          {apiKey.webhookSecret && (
            <div className="space-y-1.5 p-3 rounded-lg border bg-slate-50 dark:bg-slate-900/50">
              <div className="flex items-center justify-between">
                <Label className="text-xs font-semibold flex items-center gap-1 text-slate-700 dark:text-slate-300">
                  <ShieldCheck className="w-4 h-4 text-emerald-600" />
                  <span>Khóa bí mật Webhook (Secret Key xác thực chữ ký HMAC)</span>
                </Label>
                <div className="flex items-center gap-1">
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    className="h-7 px-2 text-xs"
                    onClick={() => setShowSecret(!showSecret)}
                  >
                    {showSecret ? <EyeOff className="w-3.5 h-3.5 mr-1" /> : <Eye className="w-3.5 h-3.5 mr-1" />}
                    {showSecret ? 'Ẩn' : 'Hiện'}
                  </Button>
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    className="h-7 px-2 text-xs"
                    onClick={handleCopySecret}
                  >
                    {copiedSecret ? <Check className="w-3.5 h-3.5 text-emerald-600 mr-1" /> : <Copy className="w-3.5 h-3.5 mr-1" />}
                    {copiedSecret ? 'Đã chép' : 'Sao chép'}
                  </Button>
                </div>
              </div>
              <div className="font-mono text-xs bg-card p-2 rounded border break-all text-slate-800 dark:text-slate-200">
                {showSecret ? apiKey.webhookSecret : apiKey.webhookSecret.replace(/^(.{8})(.*)(.{4})$/, '$1••••••••••••••••$3')}
              </div>
              <p className="text-[11px] text-muted-foreground">
                Mỗi gói tin gửi đi có Header <code className="text-[11px] font-mono">X-Webhook-Signature: t=timestamp,v1=signature</code> dùng khóa này để kiểm tra tính toàn vẹn.
              </p>
            </div>
          )}

          {/* Khu vực Test Ping */}
          <div className="pt-1">
            <div className="flex items-center justify-between mb-2">
              <span className="text-xs font-medium text-muted-foreground">Kiểm tra kết nối trực tiếp:</span>
              <Button
                type="button"
                variant="outline"
                size="sm"
                className="gap-1.5 text-xs h-8"
                onClick={handleTestPing}
                disabled={pinging || !apiKey.webhookUrl}
              >
                {pinging ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Send className="w-3.5 h-3.5 text-primary" />}
                <span>Bắn thử nghiệm (Test Ping)</span>
              </Button>
            </div>

            {/* Kết quả Test Ping */}
            {pingResult && (
              <div className={`p-3 rounded-lg border text-xs space-y-1.5 ${
                pingResult.isSuccess
                  ? 'bg-emerald-50 dark:bg-emerald-950/40 border-emerald-200 dark:border-emerald-800 text-emerald-900 dark:text-emerald-200'
                  : 'bg-rose-50 dark:bg-rose-950/40 border-rose-200 dark:border-rose-800 text-rose-900 dark:text-rose-200'
              }`}>
                <div className="flex items-center justify-between font-semibold">
                  <div className="flex items-center gap-1.5">
                    {pingResult.isSuccess ? (
                      <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                    ) : (
                      <XCircle className="w-4 h-4 text-rose-600" />
                    )}
                    <span>{pingResult.isSuccess ? 'Kết nối thành công!' : 'Kết nối thất bại!'}</span>
                  </div>
                  <div className="flex items-center gap-2">
                    {pingResult.httpStatus && (
                      <Badge variant={pingResult.isSuccess ? 'default' : 'destructive'} className="text-[11px] h-5">
                        HTTP {pingResult.httpStatus}
                      </Badge>
                    )}
                    <span className="text-[11px] font-mono text-muted-foreground">{pingResult.durationMs}ms</span>
                  </div>
                </div>
                {pingResult.errorMessage && (
                  <p className="text-[11px] text-destructive font-mono pt-1">
                    Lỗi: {pingResult.errorMessage}
                  </p>
                )}
                {pingResult.responseBody && (
                  <div className="text-[11px] font-mono bg-card/70 p-1.5 rounded border text-muted-foreground truncate max-h-16 overflow-y-auto">
                    {pingResult.responseBody}
                  </div>
                )}
              </div>
            )}
          </div>
        </div>

        <DialogFooter className="gap-2 sm:gap-0">
          <Button variant="outline" onClick={onClose} disabled={saving}>
            Hủy
          </Button>
          <Button onClick={handleSave} disabled={saving || !!urlError}>
            {saving ? (
              <>
                <Loader2 className="w-4 h-4 animate-spin mr-2" />
                <span>Đang lưu...</span>
              </>
            ) : (
              'Lưu cấu hình'
            )}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};

export default WebhookConfigModal;
