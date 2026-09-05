import { useState } from 'react';
import { z } from 'zod';
import {
  LoaderCircle,
  Send,
  AlertTriangle,
  CheckCircle2,
  Thermometer,
  Droplets,
  ScanLine,
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { ScanCodeField } from '@/components/common/ScanCodeField';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { recordStorageCondition } from '@/api/storageConditionApi';
import { scanLookupTraceCode } from '@/api/chainEventApi';
import type { StorageConditionResponse } from '@/types/storageCondition';
import { toast } from 'sonner';
import { HelpButton } from '@/components/help/HelpButton';

const formSchema = z.object({
  codeValue: z.string().min(1, 'Vui lòng nhập mã truy xuất'),
  temperature: z.coerce.number({ required_error: 'Vui lòng nhập nhiệt độ' }),
  humidity: z.coerce
    .number({ required_error: 'Vui lòng nhập độ ẩm' })
    .min(0, 'Độ ẩm phải từ 0 đến 100%')
    .max(100, 'Độ ẩm phải từ 0 đến 100%'),
});

// Hàm chuyển đổi mức cảnh báo sang tiếng Việt
const getAlertLevelLabel = (level: string) => {
  switch (level) {
    case 'CRITICAL':
      return 'Nghiêm trọng';
    case 'WARNING':
      return 'Cảnh báo';
    case 'OK':
      return 'Bình thường';
    default:
      return level;
  }
};

// Hàm trả về className cho badge dựa trên mức cảnh báo
const getAlertBadgeClasses = (level: string) => {
  switch (level) {
    case 'CRITICAL':
      return 'border-red-300 bg-red-50 text-red-700 gap-1';
    case 'WARNING':
      return 'border-yellow-300 bg-yellow-50 text-yellow-700 gap-1';
    case 'OK':
      return 'border-green-300 bg-green-50 text-green-700 gap-1';
    default:
      return 'gap-1';
  }
};

export default function StorageConditionPage() {
  const [codeValue, setCodeValue] = useState('');
  const [temperature, setTemperature] = useState('');
  const [humidity, setHumidity] = useState('');
  const [formError, setFormError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [result, setResult] = useState<StorageConditionResponse | null>(null);
  const [lotInfo, setLotInfo] = useState<{
    shipmentName: string;
    productCategoryName: string;
    farmAreaName: string;
    shipmentStatus: string;
  } | null>(null);
  const [isScanning, setIsScanning] = useState(false);
  const [scanError, setScanError] = useState<string | null>(null);

  const handleScan = async () => {
    if (!codeValue.trim()) {
      setScanError('Vui lòng nhập mã truy xuất.');
      return;
    }
    setIsScanning(true);
    setScanError(null);
    try {
      const lookupResult = await scanLookupTraceCode(codeValue.trim());
      if (!lookupResult.shipmentId) {
        setScanError('Không tìm thấy lô hàng cho mã này.');
        return;
      }
      setLotInfo({
        shipmentName: lookupResult.shipmentName,
        productCategoryName: lookupResult.productCategoryName,
        farmAreaName: lookupResult.farmAreaName,
        shipmentStatus: lookupResult.shipmentStatus,
      });
    } catch (err: any) {
      setScanError(err.response?.data?.message || 'Không thể tra cứu mã.');
    } finally {
      setIsScanning(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);
    const parsed = formSchema.safeParse({ codeValue, temperature, humidity });
    if (!parsed.success) {
      setFormError(parsed.error.issues[0]?.message ?? 'Dữ liệu không hợp lệ');
      return;
    }

    setIsSubmitting(true);
    try {
      const res = await recordStorageCondition({
        codeValue: parsed.data.codeValue,
        temperature: parsed.data.temperature,
        humidity: parsed.data.humidity,
      });
      setResult(res);
      toast.success('Đã ghi nhận điều kiện bảo quản.');
    } catch (err: any) {
      const msg = err.response?.data?.message || 'Không thể ghi nhận.';
      setFormError(msg);
      toast.error(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleReset = () => {
    setCodeValue('');
    setTemperature('');
    setHumidity('');
    setFormError(null);
    setResult(null);
    setLotInfo(null);
    setScanError(null);
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">
            Điều kiện bảo quản
          </h1>
          <p className="mt-1 text-sm text-muted-foreground">
            Ghi nhận nhiệt độ và độ ẩm trong quá trình vận chuyển. Dữ liệu mô phỏng, nhập tay.
          </p>
        </div>
        <HelpButton screenKey="storage-condition" />
      </div>

      <Card className="rounded-xl border-slate-200 bg-white shadow-sm">
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Thermometer className="size-5 text-orange-600" />
            Ghi mốc bảo quản
          </CardTitle>
          <CardDescription>
            Nhập mã truy xuất và thông số nhiệt độ, độ ẩm tại thời điểm kiểm tra.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-5">
            {/* Mã truy xuất: nút quét cùng hàng label, nút tra cứu trong input */}
            <ScanCodeField
              value={codeValue}
              onChange={(v) => { setCodeValue(v); setLotInfo(null); setScanError(null); }}
              label="Mã truy xuất *"
              placeholder="VD: 89300900000006"
              helperText="Có thể quét QR bằng camera hoặc nhập mã thủ công."
              disabled={isSubmitting}
              layout="embedded"
              scanButtonText="Quét mã QR"
              trailingAction={
                <Button
                  type="button"
                  variant="secondary"
                  onClick={handleScan}
                  disabled={isSubmitting || isScanning}
                >
                  {isScanning ? <LoaderCircle className="size-4 animate-spin" /> : <ScanLine className="size-4" />}
                  Tra cứu
                </Button>
              }
            />
            {scanError && <Alert variant="destructive"><AlertDescription>{scanError}</AlertDescription></Alert>}
            {lotInfo && (
              <div className="space-y-2 rounded-lg border border-green-200 bg-green-50 px-3 py-2.5">
                <p className="flex items-center gap-1.5 text-sm font-semibold text-green-800">
                  <CheckCircle2 className="size-4" />
                  Đã tìm thấy lô sản xuất
                </p>
                <div className="grid grid-cols-1 gap-x-4 gap-y-1 text-sm text-green-800 sm:grid-cols-2">
                  <p><span className="font-medium">Lô:</span> {lotInfo.shipmentName}</p>
                  <p><span className="font-medium">Sản phẩm:</span> {lotInfo.productCategoryName}</p>
                  <p><span className="font-medium">Vùng trồng:</span> {lotInfo.farmAreaName}</p>
                  <p><span className="font-medium">Trạng thái:</span> {lotInfo.shipmentStatus}</p>
                </div>
              </div>
            )}

            {/* Thông số bảo quản: input thường, không màu cho đến khi đánh giá */}
            <div className="space-y-3">
              <p className="text-xs font-semibold tracking-wide text-muted-foreground uppercase">
                Thông số bảo quản
              </p>
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div className="space-y-2">
                  <Label htmlFor="temperature" className="flex items-center gap-1.5">
                    <Thermometer className="size-4 text-slate-500" />
                    Nhiệt độ (°C) *
                  </Label>
                  <Input
                    id="temperature"
                    type="number"
                    step="0.1"
                    value={temperature}
                    onChange={(e) => setTemperature(e.target.value)}
                    placeholder="VD: 15.5"
                    disabled={isSubmitting}
                  />
                </div>

                <div className="space-y-2">
                  <Label htmlFor="humidity" className="flex items-center gap-1.5">
                    <Droplets className="size-4 text-slate-500" />
                    Độ ẩm (%) *
                  </Label>
                  <Input
                    id="humidity"
                    type="number"
                    step="0.1"
                    value={humidity}
                    onChange={(e) => setHumidity(e.target.value)}
                    placeholder="VD: 65.2"
                    disabled={isSubmitting}
                  />
                </div>
              </div>
            </div>

            {formError && (
              <Alert variant="destructive">
                <AlertDescription>{formError}</AlertDescription>
              </Alert>
            )}

            <div className="flex justify-end gap-2">
              <Button type="button" variant="outline" onClick={handleReset} disabled={isSubmitting}>
                Làm mới
              </Button>
              <Button type="submit" variant="view" disabled={isSubmitting}>
                {isSubmitting ? <LoaderCircle className="size-4 animate-spin" /> : <Send className="size-4" />}
                {isSubmitting ? 'Đang ghi nhận...' : 'Ghi nhận'}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>

      {/* Result */}
      {result && (
        <Card className={result.alertLevel !== 'OK' ? 'border-red-200' : 'border-emerald-200'}>
          <CardHeader className="pb-3">
            <CardTitle className="flex items-center gap-2 text-base">
              {result.alertLevel !== 'OK' ? (
                <AlertTriangle className="size-5 text-red-600" />
              ) : (
                <CheckCircle2 className="size-5 text-emerald-600" />
              )}
              Kết quả ghi nhận
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
              <div className="rounded-lg bg-blue-50 p-3">
                <p className="text-xs text-blue-700">Nhiệt độ</p>
                <p className="mt-1 text-lg font-bold text-blue-900">{result.temperature}°C</p>
              </div>
              <div className="rounded-lg bg-blue-50 p-3">
                <p className="text-xs text-blue-700">Độ ẩm</p>
                <p className="mt-1 text-lg font-bold text-blue-900">{result.humidity}%</p>
              </div>
              <div className={`rounded-lg p-3 ${result.isTemperatureExceeded ? 'bg-red-50' : 'bg-emerald-50'}`}>
                <p className={`text-xs ${result.isTemperatureExceeded ? 'text-red-700' : 'text-emerald-700'}`}>
                  Nhiệt độ
                </p>
                <p className={`mt-1 text-sm font-bold ${result.isTemperatureExceeded ? 'text-red-900' : 'text-emerald-900'}`}>
                  {result.isTemperatureExceeded ? 'Vượt ngưỡng' : 'Đạt'}
                </p>
              </div>
              <div className={`rounded-lg p-3 ${result.isHumidityExceeded ? 'bg-red-50' : 'bg-emerald-50'}`}>
                <p className={`text-xs ${result.isHumidityExceeded ? 'text-red-700' : 'text-emerald-700'}`}>
                  Độ ẩm
                </p>
                <p className={`mt-1 text-sm font-bold ${result.isHumidityExceeded ? 'text-red-900' : 'text-emerald-900'}`}>
                  {result.isHumidityExceeded ? 'Vượt ngưỡng' : 'Đạt'}
                </p>
              </div>
            </div>

            {/* Mức cảnh báo dạng badge trực quan (đã lược bỏ tiêu đề) */}
            <div className="flex items-center gap-2">
              <Badge variant="outline" className={getAlertBadgeClasses(result.alertLevel)}>
                {result.alertLevel === 'OK' ? (
                  <CheckCircle2 className="size-4" />
                ) : (
                  <AlertTriangle className="size-4" />
                )}
                {getAlertLevelLabel(result.alertLevel)}
              </Badge>
            </div>

            {result.thresholds && (
              <div className="rounded-lg bg-gray-50 p-3">
                <p className="text-xs text-gray-600 font-medium mb-2">Ngưỡng bảo quản</p>
                <div className="grid grid-cols-2 gap-2 text-xs text-gray-600">
                  <span>Nhiệt độ: {result.thresholds.tempMin}°C – {result.thresholds.tempMax}°C</span>
                  <span>Độ ẩm: {result.thresholds.humidityMin}% – {result.thresholds.humidityMax}%</span>
                </div>
              </div>
            )}

            <div className="text-xs text-muted-foreground">
              <p>Người ghi: {result.recordedBy}</p>
              <p>Thời gian: {new Date(result.recordedAt).toLocaleString('vi-VN')}</p>
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
}