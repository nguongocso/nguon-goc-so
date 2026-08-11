import { useState, useMemo } from 'react';
import { z } from 'zod';
import { LoaderCircle, Send, AlertTriangle, CheckCircle2, Package } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Card, CardContent } from '@/components/ui/card';
import { useWarehouseReceipt } from '@/hooks/useWarehouseReceipt';
import { scanLookupTraceCode } from '@/api/chainEventApi';

const ALLOWED_THRESHOLD = 2.0;

const formSchema = z.object({
  codeValue: z.string().min(1, 'Vui lòng nhập mã truy xuất'),
  receivedQuantity: z.coerce.number().positive('Số lượng thực nhận phải lớn hơn 0'),
  conditionNote: z.string().max(500, 'Ghi chú tối đa 500 ký tự').optional(),
  receiptDate: z.string().optional(),
  reason: z.string().max(500, 'Lý do tối đa 500 ký tự').optional(),
});

type FormValues = z.infer<typeof formSchema>;

interface LotInfo {
  shipmentId: string;
  shipmentName: string;
  declaredQuantity: number;
  shipmentStatus: string;
  organizationName: string;
}

export function WarehouseReceiptForm() {
  const [codeValue, setCodeValue] = useState('');
  const [receivedQuantity, setReceivedQuantity] = useState('');
  const [conditionNote, setConditionNote] = useState('');
  const [receiptDate, setReceiptDate] = useState('');
  const [reason, setReason] = useState('');
  const [formError, setFormError] = useState<string | null>(null);
  const [lotInfo, setLotInfo] = useState<LotInfo | null>(null);
  const [isScanning, setIsScanning] = useState(false);
  const [scanError, setScanError] = useState<string | null>(null);

  const { data, isLoading, error, submit, reset: resetHook } = useWarehouseReceipt();

  const declaredQuantity = lotInfo?.declaredQuantity ?? 0;
  const actualQty = parseFloat(receivedQuantity) || 0;

  const discrepancyInfo = useMemo(() => {
    if (!lotInfo || !receivedQuantity || isNaN(actualQty)) return null;
    const difference = actualQty - declaredQuantity;
    const percent = declaredQuantity === 0
      ? (actualQty > 0 ? 100 : 0)
      : (difference / declaredQuantity) * 100;
    const isExceeded = Math.abs(percent) > ALLOWED_THRESHOLD;
    return { difference, percent, isExceeded };
  }, [lotInfo, receivedQuantity, actualQty, declaredQuantity]);

  const handleScan = async () => {
    if (!codeValue.trim()) {
      setScanError('Vui lòng nhập mã truy xuất.');
      return;
    }
    setIsScanning(true);
    setScanError(null);
    setLotInfo(null);
    try {
      const result = await scanLookupTraceCode(codeValue.trim());
      if (!result.shipmentId) {
        setScanError('Không tìm thấy lô hàng cho mã truy xuất này.');
        return;
      }
      setLotInfo({
        shipmentId: result.shipmentId,
        shipmentName: result.shipmentName,
        declaredQuantity: result.totalQuantity ?? 0,
        shipmentStatus: result.shipmentStatus,
        organizationName: result.organizationName ?? '',
      });
    } catch (err: any) {
      const msg = err.response?.data?.message || 'Không thể tra cứu mã truy xuất.';
      setScanError(msg);
    } finally {
      setIsScanning(false);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      handleScan();
    }
  };

  const validate = (): FormValues | null => {
    const result = formSchema.safeParse({
      codeValue,
      receivedQuantity,
      conditionNote: conditionNote || undefined,
      receiptDate: receiptDate || undefined,
      reason: reason || undefined,
    });
    if (!result.success) {
      setFormError(result.error.issues[0]?.message ?? 'Dữ liệu không hợp lệ');
      return null;
    }

    // Business validation: reason required if discrepancy exceeds threshold
    if (discrepancyInfo?.isExceeded && (!reason || reason.trim() === '')) {
      setFormError('Chênh lệch số lượng vượt ngưỡng cho phép (2%). Vui lòng cung cấp lý do chênh lệch.');
      return null;
    }

    setFormError(null);
    return result.data;
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!lotInfo) {
      setFormError('Vui lòng tra cứu mã truy xuất trước khi nộp.');
      return;
    }
    const values = validate();
    if (!values) return;
    void submit({
      codeValue: values.codeValue,
      receivedQuantity: values.receivedQuantity,
      conditionNote: values.conditionNote,
      receiptDate: values.receiptDate || undefined,
      reason: values.reason || undefined,
    });
  };

  const handleReset = () => {
    setCodeValue('');
    setReceivedQuantity('');
    setConditionNote('');
    setReceiptDate('');
    setReason('');
    setFormError(null);
    setLotInfo(null);
    setScanError(null);
    resetHook();
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      {/* Lot code scan/input */}
      <div className="flex items-end gap-2">
        <div className="flex-1 space-y-2">
          <Label htmlFor="codeValue">Mã truy xuất (tem QR) *</Label>
          <Input
            id="codeValue"
            value={codeValue}
            onChange={(e) => { setCodeValue(e.target.value); setLotInfo(null); setScanError(null); }}
            onKeyDown={handleKeyDown}
            placeholder="VD: 89300900000006"
            disabled={isLoading}
          />
        </div>
        <Button
          type="button"
          variant="secondary"
          onClick={handleScan}
          disabled={isLoading || isScanning || !codeValue.trim()}
        >
          {isScanning ? <LoaderCircle className="size-4 animate-spin" /> : 'Tra cứu'}
        </Button>
      </div>

      {scanError && (
        <Alert variant="destructive">
          <AlertDescription>{scanError}</AlertDescription>
        </Alert>
      )}

      {/* Lot info display */}
      {lotInfo && (
        <Card className="border-blue-200 bg-blue-50">
          <CardContent className="pt-4">
            <div className="flex items-start gap-2">
              <Package className="mt-0.5 size-4 text-blue-600" />
              <div className="space-y-1 text-sm text-blue-800">
                <p className="font-semibold">{lotInfo.shipmentName}</p>
                <p>Mã lô: <span className="font-mono text-xs">{lotInfo.shipmentId}</span></p>
                <p>Đơn vị: {lotInfo.organizationName}</p>
                <p>Trạng thái: {lotInfo.shipmentStatus}</p>
                <p className="font-medium">Số lượng khai báo: <strong>{lotInfo.declaredQuantity.toLocaleString('vi-VN')} kg</strong></p>
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Received quantity */}
      <div className="space-y-2">
        <Label htmlFor="receivedQuantity">Số lượng thực nhận (kg) *</Label>
        <Input
          id="receivedQuantity"
          type="number"
          step="0.1"
          min="0.1"
          value={receivedQuantity}
          onChange={(e) => setReceivedQuantity(e.target.value)}
          placeholder="VD: 500"
          disabled={isLoading || !lotInfo}
        />
      </div>

      {/* Real-time discrepancy display */}
      {lotInfo && receivedQuantity && !isNaN(actualQty) && actualQty > 0 && discrepancyInfo && (
        <Card className={discrepancyInfo.isExceeded ? 'border-red-200 bg-red-50' : 'border-emerald-200 bg-emerald-50'}>
          <CardContent className="pt-4">
            <div className="flex items-start gap-2">
              {discrepancyInfo.isExceeded ? (
                <AlertTriangle className="mt-0.5 size-4 text-red-600" />
              ) : (
                <CheckCircle2 className="mt-0.5 size-4 text-emerald-600" />
              )}
              <div className="space-y-1 text-sm">
                <p className={`font-semibold ${discrepancyInfo.isExceeded ? 'text-red-700' : 'text-emerald-700'}`}>
                  {discrepancyInfo.isExceeded ? 'Chênh lệch vượt ngưỡng!' : 'Chênh lệch trong ngưỡng cho phép'}
                </p>
                <div className={`grid grid-cols-2 gap-x-4 gap-y-0.5 ${discrepancyInfo.isExceeded ? 'text-red-700' : 'text-emerald-700'}`}>
                  <span>Số lượng khai báo:</span>
                  <span className="font-medium">{declaredQuantity.toLocaleString('vi-VN')} kg</span>
                  <span>Số lượng thực nhận:</span>
                  <span className="font-medium">{actualQty.toLocaleString('vi-VN')} kg</span>
                  <span>Chênh lệch:</span>
                  <span className="font-medium">{discrepancyInfo.difference >= 0 ? '+' : ''}{Math.round(discrepancyInfo.difference * 100) / 100} kg</span>
                  <span>% Chênh lệch:</span>
                  <span className="font-medium">{discrepancyInfo.difference >= 0 ? '+' : ''}{Math.round(discrepancyInfo.percent * 100) / 100}%</span>
                  <span>Ngưỡng cho phép:</span>
                  <span className="font-medium">{ALLOWED_THRESHOLD}%</span>
                </div>
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Condition note */}
      <div className="space-y-2">
        <Label htmlFor="conditionNote">Tình trạng hàng hóa</Label>
        <Textarea
          id="conditionNote"
          value={conditionNote}
          onChange={(e) => setConditionNote(e.target.value)}
          placeholder="Mô tả tình trạng hàng khi nhập kho..."
          rows={3}
          disabled={isLoading || !lotInfo}
        />
        <p className="text-xs text-muted-foreground">{conditionNote.length}/500</p>
      </div>

      {/* Receipt date */}
      <div className="space-y-2">
        <Label htmlFor="receiptDate">Ngày nhập kho</Label>
        <Input
          id="receiptDate"
          type="date"
          value={receiptDate}
          onChange={(e) => setReceiptDate(e.target.value)}
          disabled={isLoading || !lotInfo}
        />
        <p className="text-xs text-muted-foreground">Để trống để sử dụng ngày hiện tại</p>
      </div>

      {/* Discrepancy reason (conditionally required) */}
      <div className="space-y-2">
        <Label htmlFor="reason" className="flex items-center gap-1">
          Lý do chênh lệch
          {discrepancyInfo?.isExceeded && (
            <span className="text-red-500">*</span>
          )}
          {!discrepancyInfo?.isExceeded && (
            <span className="text-muted-foreground text-xs font-normal">(không bắt buộc)</span>
          )}
        </Label>
        <Textarea
          id="reason"
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          placeholder={discrepancyInfo?.isExceeded ? 'Bắt buộc nhập lý do khi chênh lệch vượt ngưỡng 2%...' : 'Nhập lý do nếu có chênh lệch...'}
          rows={3}
          disabled={isLoading || !lotInfo}
        />
        <p className="text-xs text-muted-foreground">{reason.length}/500</p>
      </div>

      {/* Form-level errors */}
      {(formError || error) && (
        <Alert variant="destructive">
          <AlertDescription>{formError || error}</AlertDescription>
        </Alert>
      )}

      {/* Submit */}
      <div className="flex gap-2">
        <Button type="submit" variant="view" disabled={isLoading || !lotInfo} className="flex-1">
          {isLoading ? (
            <LoaderCircle className="size-4 animate-spin" />
          ) : (
            <Send className="size-4" />
          )}
          {isLoading ? 'Đang ghi nhận...' : 'Ghi nhận nhập kho'}
        </Button>
        <Button type="button" variant="outline" onClick={handleReset} disabled={isLoading}>
          Làm mới
        </Button>
      </div>

      {/* Success result */}
      {data && (
        <Card className="border-emerald-200 bg-emerald-50">
          <CardContent className="pt-6">
            <p className="font-semibold text-emerald-800">Nhập kho thành công!</p>
            <div className="mt-2 space-y-1 text-sm text-emerald-700">
              <p>Mã sự kiện: <span className="font-mono">{data.id}</span></p>
              <p>Lô hàng: {data.shipmentName}</p>
              <p>Số lượng khai báo: {data.declaredQuantity.toLocaleString('vi-VN')} kg</p>
              <p>Số lượng thực nhận: {data.receivedQuantity.toLocaleString('vi-VN')} kg</p>
              <p>Chênh lệch: {data.discrepancy >= 0 ? '+' : ''}{data.discrepancy} kg ({data.discrepancyPercent >= 0 ? '+' : ''}{data.discrepancyPercent}%)</p>
              {data.reason && <p>Lý do: {data.reason}</p>}
              {data.notificationSent && (
                <p className="text-amber-700">Đã gửi thông báo chênh lệch đến hợp tác xã.</p>
              )}
              <p>Người ghi: {data.recordedBy}</p>
              <p>Thời gian: {new Date(data.recordedAt).toLocaleString('vi-VN')}</p>
            </div>
          </CardContent>
        </Card>
      )}
    </form>
  );
}