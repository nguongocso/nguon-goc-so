import {
  AlertTriangle,
  Droplets,
  LoaderCircle,
  ScanLine,
  Send,
  Thermometer,
} from 'lucide-react';

import { LotLookupResult } from '@/components/common/LotLookupResult';
import { ScanCodeField } from '@/components/common/ScanCodeField';
import { getShipmentStatusLabel } from '@/components/shipment/ShipmentStatusBadge';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import type { StorageConditionController } from './useStorageCondition';

interface StorageConditionFormProps {
  controller: StorageConditionController;
}

/** Biểu mẫu tra cứu lô và nhập thông số bảo quản. */
export function StorageConditionForm({ controller }: StorageConditionFormProps) {
  const {
    blockedMessage, changeCodeValue, codeValue, formError, handleReset, handleScan,
    handleSubmit, humidity, isBlocked, isScanning, isSubmitting, lotInfo,
    recordDisabled, scanError, setHumidity, setTemperature, temperature,
  } = controller;

  return (
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
          <ScanCodeField
            value={codeValue}
            onChange={changeCodeValue}
            label="Mã truy xuất *"
            placeholder="VD: 89300900000006"
            helperText="Quét QR sẽ tự tra cứu. Nhập tay rồi bấm Tra cứu."
            disabled={isSubmitting}
            layout="embedded"
            scanButtonText="Quét mã QR"
            onScanComplete={(code) => void handleScan(code)}
            trailingAction={(
              <Button
                type="button"
                variant="secondary"
                onClick={() => void handleScan()}
                disabled={isSubmitting || isScanning}
              >
                {isScanning
                  ? <LoaderCircle className="size-4 animate-spin" />
                  : <ScanLine className="size-4" />}
                Tra cứu
              </Button>
            )}
          />

          {scanError && (
            <Alert variant="destructive"><AlertDescription>{scanError}</AlertDescription></Alert>
          )}
          {lotInfo && isBlocked && (
            <Alert className="border-amber-200 bg-amber-50">
              <AlertTriangle className="size-4 text-amber-600" />
              <AlertDescription className="text-amber-800">{blockedMessage}</AlertDescription>
            </Alert>
          )}
          {lotInfo && !isBlocked && (
            <LotLookupResult
              items={[
                { label: 'Lô', value: lotInfo.shipmentName },
                { label: 'Sản phẩm', value: lotInfo.productCategoryName },
                { label: 'Vùng trồng', value: lotInfo.farmAreaName },
                { label: 'Trạng thái', value: getShipmentStatusLabel(lotInfo.shipmentStatus) },
              ]}
            />
          )}

          <div className="space-y-3">
            <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
              Thông số bảo quản
            </p>
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <div className="space-y-2">
                <Label htmlFor="temperature" className="flex items-center gap-1.5">
                  <Thermometer className="size-4 text-slate-500" /> Nhiệt độ (°C) *
                </Label>
                <Input
                  id="temperature"
                  type="number"
                  step="0.1"
                  value={temperature}
                  onChange={(event) => setTemperature(event.target.value)}
                  placeholder="VD: 15.5"
                  disabled={recordDisabled}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="humidity" className="flex items-center gap-1.5">
                  <Droplets className="size-4 text-slate-500" /> Độ ẩm (%) *
                </Label>
                <Input
                  id="humidity"
                  type="number"
                  step="0.1"
                  value={humidity}
                  onChange={(event) => setHumidity(event.target.value)}
                  placeholder="VD: 65.2"
                  disabled={recordDisabled}
                />
              </div>
            </div>
          </div>

          {formError && (
            <Alert variant="destructive"><AlertDescription>{formError}</AlertDescription></Alert>
          )}
          <div className="flex justify-end gap-2">
            <Button
              type="button"
              variant="outline"
              onClick={handleReset}
              disabled={isSubmitting}
            >
              Làm mới
            </Button>
            <Button
              type="submit"
              variant="view"
              disabled={recordDisabled}
            >
              {isSubmitting ? <LoaderCircle className="size-4 animate-spin" /> : <Send className="size-4" />}
              {isSubmitting ? 'Đang ghi nhận...' : 'Ghi nhận'}
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  );
}
