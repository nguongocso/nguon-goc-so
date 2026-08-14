import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { AlertTriangle, LoaderCircle } from 'lucide-react';
import { getProductionLots } from '@/api/productionLotApi';
import { createRecallRequest } from '@/api/recallApi';
import type { ProductionLot } from '@/types/productionLot';

const RECALLABLE_STATUSES = ['APPROVED', 'HARVESTED', 'PACKAGED'];

export const CreateRecallRequestPage = () => {
  const navigate = useNavigate();
  const [lots, setLots] = useState<ProductionLot[]>([]);
  const [selectedLotId, setSelectedLotId] = useState('');
  const [reason, setReason] = useState('');
  const [evidence, setEvidence] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [loadingLots, setLoadingLots] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const loadLots = async () => {
      try {
        setLoadingLots(true);
        const allLots = await getProductionLots();
        const recallable = allLots.filter((lot) =>
          RECALLABLE_STATUSES.includes(lot.status),
        );
        setLots(recallable);
        if (recallable.length > 0 && !selectedLotId) {
          setSelectedLotId(recallable[0].id);
        }
      } catch {
        toast.error('Không thể tải danh sách lô sản xuất');
      } finally {
        setLoadingLots(false);
      }
    };
    loadLots();
  }, []);

  const handleSubmit = async () => {
    setError(null);

    if (!selectedLotId) {
      setError('Vui lòng chọn lô sản xuất.');
      return;
    }
    if (!reason.trim()) {
      setError('Lý do thu hồi không được để trống.');
      return;
    }

    try {
      setSubmitting(true);
      const result = await createRecallRequest({
        lotId: selectedLotId,
        reason: reason.trim(),
        evidence: evidence.trim() || undefined,
      });
      toast.success('Đã gửi yêu cầu thu hồi thành công. Chờ quản lý duyệt.');
      navigate('/recall-requests');
    } catch (err: any) {
      const message =
        err.response?.data?.message || 'Không thể tạo yêu cầu thu hồi.';
      toast.error(message);
      setError(message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="container mx-auto max-w-2xl py-6 space-y-6">
      <Card className="border-red-100">
        <CardHeader>
          <div className="flex items-center gap-3">
            <div className="flex size-11 items-center justify-center rounded-full bg-red-100 text-red-700">
              <AlertTriangle className="size-6" />
            </div>
            <div>
              <CardTitle className="text-xl font-bold">
                Tạo yêu cầu thu hồi lô sản xuất
              </CardTitle>
              <p className="text-sm text-muted-foreground mt-1">
                Gửi yêu cầu để quản lý hợp tác xã xét duyệt. Yêu cầu sẽ được duyệt
                bởi người khác, không phải chính bạn.
              </p>
            </div>
          </div>
        </CardHeader>
        <CardContent className="space-y-5">
          <div className="space-y-1.5">
            <Label htmlFor="lotId">
              Lô sản xuất <span className="text-red-600">*</span>
            </Label>
            {loadingLots ? (
              <p className="text-sm text-muted-foreground">Đang tải lô...</p>
            ) : lots.length === 0 ? (
              <p className="text-sm text-amber-600">
                Không có lô sản xuất đang hiệu lực nào để thu hồi.
              </p>
            ) : (
              <select
                id="lotId"
                value={selectedLotId}
                onChange={(e) => setSelectedLotId(e.target.value)}
                className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
              >
                {lots.map((lot) => (
                  <option key={lot.id} value={lot.id}>
                    {lot.name} ({lot.status})
                  </option>
                ))}
              </select>
            )}
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="reason">
              Lý do thu hồi <span className="text-red-600">*</span>
            </Label>
            <Textarea
              id="reason"
              placeholder="VD: Phát hiện dư lượng thuốc bảo vệ thực vật vượt ngưỡng cho phép"
              value={reason}
              rows={4}
              onChange={(e) => {
                setReason(e.target.value);
                if (error) setError(null);
              }}
            />
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="evidence">Bằng chứng (tùy chọn)</Label>
            <Textarea
              id="evidence"
              placeholder="Mô tả bằng chứng kèm theo (kết quả xét nghiệm, hình ảnh, ...)"
              value={evidence}
              rows={3}
              onChange={(e) => setEvidence(e.target.value)}
            />
          </div>

          {error && <p className="text-sm text-red-600">{error}</p>}

          <div className="flex items-center justify-end gap-2">
            <Button
              variant="outline"
              onClick={() => navigate(-1)}
              disabled={submitting}
            >
              Hủy
            </Button>
            <Button
              variant="delete"
              onClick={() => void handleSubmit()}
              disabled={submitting || lots.length === 0}
            >
              {submitting && <LoaderCircle className="size-4 animate-spin" />}
              {submitting ? 'Đang gửi...' : 'Gửi yêu cầu thu hồi'}
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};