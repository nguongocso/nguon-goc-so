import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import type { ScanAnomalyAlert } from '@/types/scanAnomalyAlert';
import { DetailSection } from '@/components/common/detail/DetailSection';
import { DetailField } from '@/components/common/detail/DetailField';
import { AlertTriangle, CheckCircle2, MapPin } from 'lucide-react';

interface Props {
  alert: ScanAnomalyAlert | null;
  onClose: () => void;
  onResolve: (alert: ScanAnomalyAlert) => void;
}

const formatDateTime = (value: string | null) => {
  if (!value) return '—';
  return new Date(value).toLocaleString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  });
};

export function ScanAnomalyAlertDetailsDialog({
  alert,
  onClose,
  onResolve,
}: Props) {
  return (
    <Dialog open={Boolean(alert)} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-h-[85vh] overflow-y-auto sm:max-w-lg md:max-w-2xl lg:max-w-3xl">
        {alert && (
          <>
            <DialogHeader>
              <div className="flex items-start gap-3 pr-8">
                <div className="rounded-full bg-amber-100 p-2 text-amber-700">
                  <AlertTriangle className="h-5 w-5" />
                </div>
                <div className="space-y-1">
                  <DialogTitle>Chi tiết cảnh báo tem quét bất thường</DialogTitle>
                  <DialogDescription>
                    Mã cảnh báo: <span className="font-mono">{alert.id}</span>
                  </DialogDescription>
                </div>
              </div>
            </DialogHeader>

            <DetailSection
              title="Tổng quan cảnh báo"
              contentClassName="grid gap-3 sm:grid-cols-2"
            >
              <DetailField
                label="Mức độ"
                value={
                  <Badge
                    variant={alert.severity === 'HIGH' ? 'destructive' : 'outline'}
                    className={alert.severity === 'MEDIUM' ? 'border-amber-300 text-amber-700' : undefined}
                  >
                    {alert.severity === 'HIGH' ? 'Cao' : 'Trung bình'}
                  </Badge>
                }
              />
              <DetailField
                label="Trạng thái"
                value={
                  <Badge variant={alert.status === 'PENDING' ? 'secondary' : 'outline'}>
                    {alert.status === 'PENDING' ? 'Chờ xử lý' : 'Đã xử lý'}
                  </Badge>
                }
              />
              <DetailField label="Mã TraceCode" mono value={alert.relatedEntityId} />
              <DetailField label="Thời điểm tạo" value={formatDateTime(alert.createdAt)} />
              <DetailField label="Số lượt quét" value={`${alert.details.scanCount} lượt`} />
              <DetailField
                label="Ngưỡng cấu hình"
                value={`${alert.details.thresholdConfigured} vị trí`}
              />
              {alert.status === 'RESOLVED' && (
                <>
                  <DetailField label="Xử lý lúc" value={formatDateTime(alert.resolvedAt)} />
                  <DetailField label="Người xử lý" mono value={alert.resolvedBy || undefined} />
                </>
              )}
            </DetailSection>

            <DetailSection
              title={`Các vị trí đã quét (${alert.details.locations.length})`}
              icon={<MapPin className="h-4 w-4 text-amber-600" />}
              contentClassName="overflow-hidden p-0"
            >
              <div className="overflow-x-auto bg-card">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead className="w-14">STT</TableHead>
                      <TableHead>Vĩ độ</TableHead>
                      <TableHead>Kinh độ</TableHead>
                      <TableHead>Thời gian quét</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {alert.details.locations.map((location, index) => (
                      <TableRow
                        key={`${location.latitude}-${location.longitude}-${location.scannedAt}`}
                      >
                        <TableCell>{index + 1}</TableCell>

                        <TableCell className="font-mono text-xs">
                          {location.latitude != null
                            ? location.latitude.toFixed(6)
                            : '—'}
                        </TableCell>

                        <TableCell className="font-mono text-xs">
                          {location.longitude != null
                            ? location.longitude.toFixed(6)
                            : '—'}
                        </TableCell>

                        <TableCell className="whitespace-nowrap text-sm">
                          {formatDateTime(location.scannedAt)}
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>
            </DetailSection>

            <DialogFooter>
              <Button variant="outline" onClick={onClose}>Đóng</Button>
              {alert.status === 'PENDING' && (
                <Button
                  onClick={() => onResolve(alert)}
                  variant="edit"
                >
                  <CheckCircle2 className="h-4 w-4" />
                  Xử lý cảnh báo
                </Button>
              )}
            </DialogFooter>
          </>
        )}
      </DialogContent>
    </Dialog>
  );
}