import { LoaderCircle, Package, ScanLine } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Alert, AlertDescription } from '@/components/ui/alert';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { ScanCodeField } from '@/components/common/ScanCodeField';
import { LotLookupResult } from '@/components/common/LotLookupResult';
import { getShipmentStatusLabel } from '@/components/shipment/ShipmentStatusBadge';
import { selectAllOnFocus, preventMouseUpCollapse } from '@/utils/inputUtils';
import { WarehouseReceiptDiscrepancy } from './WarehouseReceiptDiscrepancy';
import { WarehouseReceiptDialogFooter } from './WarehouseReceiptDialogFooter';
import { useWarehouseReceiptCreateForm } from './useWarehouseReceiptCreateForm';

/** Thuộc tính cho hộp thoại tạo phiếu nhập kho. */
export interface WarehouseReceiptCreateDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onCreated: () => void;
}

/** Hộp thoại tra cứu mã truy xuất và xác nhận ghi nhận nhập kho. */
export function WarehouseReceiptCreateDialog({
  open,
  onOpenChange,
  onCreated,
}: WarehouseReceiptCreateDialogProps) {
  const form = useWarehouseReceiptCreateForm({ onOpenChange, onCreated });
  const {
    codeValue,
    receivedQuantity,
    conditionNote,
    receiptDate,
    reason,
    formError,
    lotInfo,
    isScanning,
    scanError,
    isSubmitting,
    error,
    actualQty,
    discrepancyInfo,
  } = form;

  return (
    <Dialog open={open} onOpenChange={form.handleOpenChange}>
      <DialogContent className="max-h-[90vh] max-w-lg overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Package className="size-5 text-blue-700" />
            Nhập kho
          </DialogTitle>
          <DialogDescription>
            Quét hoặc nhập mã truy xuất, sau đó nhập số lượng thực nhận.
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={form.handleSubmit} className="space-y-5">
          <ScanCodeField
            value={codeValue}
            onChange={form.handleCodeChange}
            label="Mã truy xuất (tem QR) *"
            placeholder="VD: 89300900000006"
            helperText="Quét QR sẽ tự tra cứu. Nhập tay rồi bấm Tra cứu."
            disabled={isSubmitting}
            layout="embedded"
            scanButtonText="Quét mã QR"
            onScanComplete={(code) => void form.handleScan(code)}
            trailingAction={
              <Button
                type="button"
                variant="secondary"
                onClick={() => void form.handleScan()}
                disabled={isSubmitting || isScanning || !codeValue.trim()}
              >
                {isScanning ? (
                  <LoaderCircle className="size-4 animate-spin" />
                ) : (
                  <ScanLine className="size-4" />
                )}
                Tra cứu
              </Button>
            }
          />

          {scanError && (
            <Alert variant="destructive">
              <AlertDescription>{scanError}</AlertDescription>
            </Alert>
          )}

          {lotInfo && (
            <LotLookupResult
              items={[
                { label: 'Lô', value: lotInfo.shipmentName },
                {
                  label: 'Trạng thái',
                  value: getShipmentStatusLabel(lotInfo.shipmentStatus),
                },
                { label: 'Đơn vị', value: lotInfo.organizationName },
                {
                  label: 'Số lượng khai báo',
                  value: `${lotInfo.declaredQuantity.toLocaleString('vi-VN')} kg`,
                },
              ]}
            />
          )}

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
              onChange={(event) => form.setReceivedQuantity(event.target.value)}
              placeholder="VD: 500"
              disabled={isSubmitting || !lotInfo}
            />
          </div>

          {lotInfo && receivedQuantity && !Number.isNaN(actualQty) && actualQty > 0 && discrepancyInfo && (
            <WarehouseReceiptDiscrepancy info={discrepancyInfo} />
          )}

          <div className="space-y-2">
            <Label htmlFor="conditionNote">Tình trạng hàng hóa</Label>
            <Textarea
              id="conditionNote"
              value={conditionNote}
              onChange={(event) => form.setConditionNote(event.target.value)}
              placeholder="Mô tả tình trạng hàng khi nhập kho..."
              rows={2}
              disabled={isSubmitting || !lotInfo}
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="receiptDate">Ngày nhập kho</Label>
            <Input
              id="receiptDate"
              type="date"
              value={receiptDate}
              onChange={(event) => form.setReceiptDate(event.target.value)}
              disabled={isSubmitting || !lotInfo}
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="reason" className="flex items-center gap-1">
              Lý do chênh lệch
              {discrepancyInfo?.isExceeded && <span className="text-red-500">*</span>}
            </Label>
            <Textarea
              id="reason"
              value={reason}
              onChange={(event) => form.setReason(event.target.value)}
              placeholder={
                discrepancyInfo?.isExceeded
                  ? 'Bắt buộc khi chênh lệch vượt 2%...'
                  : 'Không bắt buộc'
              }
              rows={2}
              disabled={isSubmitting || !lotInfo}
            />
          </div>

          {(formError || error) && (
            <Alert variant="destructive">
              <AlertDescription>{formError || error}</AlertDescription>
            </Alert>
          )}

          <WarehouseReceiptDialogFooter
            isSubmitting={isSubmitting}
            canSubmit={Boolean(lotInfo)}
            onCancel={() => form.handleOpenChange(false)}
          />
        </form>
      </DialogContent>
    </Dialog>
  );
}
