import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import { AlertTriangle, LoaderCircle } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { HelpButton } from '@/components/help/HelpButton';
import { getProductionLots } from '@/api/productionLotApi';
import { getShipmentsByProductionLot } from '@/api/shipmentApi';
import { createRecallRequest } from '@/api/recallApi';
import type { ProductionLot } from '@/types/productionLot';
import type { Shipment } from '@/types/shipment';
import { PRODUCTION_LOT_STATUS_LABELS } from '@/components/production-lot/ProductionLotStatusBadge';

const RECALLABLE_LOT_STATUSES = ['APPROVED', 'HARVESTED', 'PACKAGED'];

export const CreateRecallRequestPage = () => {
  const navigate = useNavigate();
  const [lots, setLots] = useState<ProductionLot[]>([]);
  const [shipments, setShipments] = useState<Shipment[]>([]);
  const [selectedLotId, setSelectedLotId] = useState('');
  const [selectedShipmentId, setSelectedShipmentId] = useState('');
  const [reason, setReason] = useState('');
  const [evidence, setEvidence] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [loadingLots, setLoadingLots] = useState(true);
  const [loadingShipments, setLoadingShipments] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getProductionLots()
      .then((items) => {
        const recallable = items.filter((lot) => RECALLABLE_LOT_STATUSES.includes(lot.status));
        setLots(recallable);
        setSelectedLotId(recallable[0]?.id ?? '');
      })
      .catch(() => toast.error('Không thể tải danh sách lô sản xuất'))
      .finally(() => setLoadingLots(false));
  }, []);

  useEffect(() => {
    if (!selectedLotId) {
      setShipments([]);
      setSelectedShipmentId('');
      return;
    }
    let cancelled = false;
    setLoadingShipments(true);
    getShipmentsByProductionLot(selectedLotId)
      .then((items) => {
        if (cancelled) return;
        const recallable = items.filter(
          (shipment) => shipment.status !== 'RECALLED' && shipment.status !== 'SPLIT',
        );
        setShipments(recallable);
        setSelectedShipmentId(recallable[0]?.id ?? '');
      })
      .catch(() => {
        if (!cancelled) {
          setShipments([]);
          setSelectedShipmentId('');
          toast.error('Không thể tải danh sách lô hàng');
        }
      })
      .finally(() => !cancelled && setLoadingShipments(false));
    return () => { cancelled = true; };
  }, [selectedLotId]);

  const handleSubmit = async () => {
    setError(null);
    if (!selectedShipmentId) {
      setError('Vui lòng chọn lô hàng cần thu hồi.');
      return;
    }
    if (!reason.trim()) {
      setError('Lý do thu hồi không được để trống.');
      return;
    }
    try {
      setSubmitting(true);
      await createRecallRequest({
        shipmentId: selectedShipmentId,
        reason: reason.trim(),
        evidence: evidence.trim() || undefined,
      });
      toast.success('Yêu cầu thu hồi lô hàng đã được gửi thành công.');
      setReason('');
      setEvidence('');
    } catch (err: any) {
      const message = err.response?.data?.message || 'Không thể tạo yêu cầu thu hồi.';
      toast.error(message);
      setError(message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="flex items-center gap-2 text-2xl font-bold tracking-tight text-slate-900">
            <AlertTriangle className="size-6 text-red-600" />
            Tạo yêu cầu thu hồi lô hàng
          </h1>
          <p className="mt-1 text-sm text-muted-foreground">
            Chọn đúng lô hàng cần thu hồi. Khi được duyệt, toàn bộ mã tem của lô hàng này sẽ bị thu hồi.
          </p>
        </div>
        <HelpButton screenKey="recall-request-create" />
      </div>

      <Card className="rounded-xl border-slate-200 bg-white shadow-sm">
        <CardContent className="space-y-5 pt-6">
          <div className="space-y-1.5">
            <Label htmlFor="lotId">Lô sản xuất (để lọc)</Label>
            {loadingLots ? <p className="text-sm text-muted-foreground">Đang tải...</p> : (
              <select id="lotId" value={selectedLotId} onChange={(e) => setSelectedLotId(e.target.value)} className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm">
                {lots.map((lot) => <option key={lot.id} value={lot.id}>{lot.name} ({PRODUCTION_LOT_STATUS_LABELS[lot.status] || lot.status})</option>)}
              </select>
            )}
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="shipmentId">Lô hàng cần thu hồi <span className="text-red-600">*</span></Label>
            {loadingShipments ? <p className="text-sm text-muted-foreground">Đang tải lô hàng...</p> : shipments.length === 0 ? (
              <p className="text-sm text-amber-600">Lô sản xuất này không có lô hàng phù hợp để thu hồi.</p>
            ) : (
              <select id="shipmentId" value={selectedShipmentId} onChange={(e) => setSelectedShipmentId(e.target.value)} className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm">
                {shipments.map((shipment) => <option key={shipment.id} value={shipment.id}>{shipment.name} ({shipment.traceCodes.length} mã tem)</option>)}
              </select>
            )}
          </div>

          <div className="space-y-1.5"><Label htmlFor="reason">Lý do thu hồi <span className="text-red-600">*</span></Label><Textarea id="reason" value={reason} rows={4} maxLength={1000} onChange={(e) => setReason(e.target.value)} /></div>
          <div className="space-y-1.5"><Label htmlFor="evidence">Bằng chứng (tùy chọn)</Label><Textarea id="evidence" value={evidence} rows={3} maxLength={2000} onChange={(e) => setEvidence(e.target.value)} /></div>
          {error && <p className="text-sm text-red-600">{error}</p>}
          <div className="flex justify-end gap-2">
            <Button variant="outline" onClick={() => navigate(-1)} disabled={submitting}>Hủy</Button>
            <Button variant="delete" onClick={() => void handleSubmit()} disabled={submitting || !selectedShipmentId}>
              {submitting && <LoaderCircle className="size-4 animate-spin" />}{submitting ? 'Đang gửi...' : 'Gửi yêu cầu thu hồi'}
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};
