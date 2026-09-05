import { useState, useMemo } from 'react';
import { z } from 'zod';
import { LoaderCircle, Send, AlertTriangle, CheckCircle2, Package, ScanLine } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Card, CardContent } from '@/components/ui/card';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { useWarehouseReceipt } from '@/hooks/useWarehouseReceipt';
import { scanLookupTraceCode } from '@/api/chainEventApi';
import { ScanCodeField } from '@/components/common/ScanCodeField';
import { getLocalDateString } from '@/utils/dateTime';
import { selectAllOnFocus, preventMouseUpCollapse } from '@/utils/inputUtils';

const ALLOWED_THRESHOLD = 2.0;

const formSchema = z.object({
  codeValue: z.string().min(1, 'Vui lòng nhập mã truy xuất'),
  receivedQuantity: z.coerce.number().positive('Số lượng thực nhận phải lớn hơn 0'),
  conditionNote: z.string().max(500, 'Ghi chú tối đa 500 ký tự').optional(),
  receiptDate: z.string().optional(),
  reason: z.string().max(500, 'Lý do tối đa 500 ký tự').optional(),
});

interface LotInfo {
  shipmentId: string;
  shipmentName: string;
  declaredQuantity: number;
  shipmentStatus: string;
  organizationName: string;
}

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onCreated: () => void;
}

export function WarehouseReceiptCreateDialog({ open, onOpenChange, onCreated }: Props) {
  // 👇 Lấy ngày hôm nay theo giờ local (tránh lệch ngày UTC)
  const today = getLocalDateString();

  const [codeValue, setCodeValue] = useState('');
  const [receivedQuantity, setReceivedQuantity] = useState('');
  const [conditionNote, setConditionNote] = useState('');
  // 👇 Set mặc định là ngày hôm nay
  const [receiptDate, setReceiptDate] = useState(today);
  const [reason, setReason] = useState('');
  const [formError, setFormError] = useState<string | null>(null);
  const [lotInfo, setLotInfo] = useState<LotInfo | null>(null);
  const [isScanning, setIsScanning] = useState(false);
  const [scanError, setScanError] = useState<string | null>(null);

  const { isSubmitting, error, submitReceipt } = useWarehouseReceipt();

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
      if (result.shipmentStatus !== 'ACTIVATED') {
        setScanError('Lô hàng chưa được kích hoạt hoặc đã bị thu hồi.');
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

  const validate = (): boolean => {
    const result = formSchema.safeParse({
      codeValue,
      receivedQuantity,
      conditionNote: conditionNote || undefined,
      receiptDate: receiptDate || undefined,
      reason: reason || undefined,
    });
    if (!result.success) {
      setFormError(result.error.issues[0]?.message ?? 'Dữ liệu không hợp lệ');
      return false;
    }
    if (discrepancyInfo?.isExceeded && (!reason || reason.trim() === '')) {
      setFormError('Chênh lệch số lượng vượt ngưỡng cho phép (2%). Vui lòng cung cấp lý do chênh lệch.');
      return false;
    }
    setFormError(null);
    return true;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!lotInfo) {
      setFormError('Vui lòng tra cứu mã truy xuất trước.');
      return;
    }
    if (!validate()) return;
    const success = await submitReceipt({
      codeValue,
      receivedQuantity: parseFloat(receivedQuantity),
      conditionNote: conditionNote || undefined,
      receiptDate: receiptDate || undefined,
      reason: reason || undefined,
    });
    if (success) {
      resetForm();
      onCreated();
    }
  };

  const resetForm = () => {
    setCodeValue('');
    setReceivedQuantity('');
    setConditionNote('');
    setReceiptDate(today); // 👈 Reset về ngày hôm nay
    setReason('');
    setFormError(null);
    setLotInfo(null);
    setScanError(null);
  };

  const handleOpenChange = (open: boolean) => {
    if (!open) resetForm();
    onOpenChange(open);
  };

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="max-h-[90vh] max-w-lg overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Package className="size-5 text-blue-700" />
            Nhập kho & Đối chiếu
          </DialogTitle>
          <DialogDescription>
            Quét hoặc nhập mã truy xuất, sau đó nhập số lượng thực nhận.
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-5">
          {/* Mã truy xuất: nút quét cùng hàng label, nút tra cứu trong input */}
          <ScanCodeField
            value={codeValue}
            onChange={(v) => { setCodeValue(v); setLotInfo(null); setScanError(null); }}
            label="Mã truy xuất (tem QR) *"
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
                disabled={isSubmitting || isScanning || !codeValue.trim()}
              >
                {isScanning ? <LoaderCircle className="size-4 animate-spin" /> : <ScanLine className="size-4" />}
                Tra cứu
              </Button>
            }
          />

          {scanError && (
            <Alert variant="destructive">
              <AlertDescription>{scanError}</AlertDescription>
            </Alert>
          )}

          {/* Lot info */}
          {lotInfo && (
            <Card className="border-blue-200 bg-blue-50">
              <CardContent className="p-3">
                <div className="space-y-1 text-sm text-blue-800">
                  <p className="font-semibold">{lotInfo.shipmentName}</p>
                  <p>Đơn vị: {lotInfo.organizationName}</p>
                  <p className="font-medium">
                    Số lượng khai báo: <strong>{lotInfo.declaredQuantity.toLocaleString('vi-VN')} kg</strong>
                  </p>
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
              onFocus={selectAllOnFocus}
              onMouseUp={preventMouseUpCollapse}
              onChange={(e) => setReceivedQuantity(e.target.value)}
              placeholder="VD: 500"
              disabled={isSubmitting || !lotInfo}
            />
          </div>

          {/* Discrepancy display */}
          {lotInfo && receivedQuantity && !isNaN(actualQty) && actualQty > 0 && discrepancyInfo && (
            <Card className={discrepancyInfo.isExceeded ? 'border-red-200 bg-red-50' : 'border-emerald-200 bg-emerald-50'}>
              <CardContent className="p-3">
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
                      <span>Chênh lệch:</span>
                      <span className="font-medium">{discrepancyInfo.difference >= 0 ? '+' : ''}{Math.round(discrepancyInfo.difference * 100) / 100} kg</span>
                      <span>% Chênh lệch:</span>
                      <span className="font-medium">{discrepancyInfo.difference >= 0 ? '+' : ''}{Math.round(discrepancyInfo.percent * 100) / 100}%</span>
                      <span>Ngưỡng:</span>
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
              rows={2}
              disabled={isSubmitting || !lotInfo}
            />
          </div>

          {/* Receipt date - mặc định là ngày hôm nay */}
          <div className="space-y-2">
            <Label htmlFor="receiptDate">Ngày nhập kho</Label>
            <Input
              id="receiptDate"
              type="date"
              value={receiptDate}
              onChange={(e) => setReceiptDate(e.target.value)}
              disabled={isSubmitting || !lotInfo}
            />
          </div>

          {/* Discrepancy reason */}
          <div className="space-y-2">
            <Label htmlFor="reason" className="flex items-center gap-1">
              Lý do chênh lệch
              {discrepancyInfo?.isExceeded && <span className="text-red-500">*</span>}
            </Label>
            <Textarea
              id="reason"
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              placeholder={discrepancyInfo?.isExceeded ? 'Bắt buộc khi chênh lệch vượt 2%...' : 'Không bắt buộc'}
              rows={2}
              disabled={isSubmitting || !lotInfo}
            />
          </div>

          {(formError || error) && (
            <Alert variant="destructive">
              <AlertDescription>{formError || error}</AlertDescription>
            </Alert>
          )}

          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              onClick={() => handleOpenChange(false)}
              disabled={isSubmitting}
            >
              Hủy
            </Button>
            <Button type="submit" variant="view" disabled={isSubmitting || !lotInfo}>
              {isSubmitting ? (
                <>
                  <LoaderCircle className="size-4 animate-spin" />
                  Đang ghi nhận...
                </>
              ) : (
                <>
                  <Send className="size-4" />
                  Xác nhận nhập kho
                </>
              )}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}