import React, { useCallback, useEffect, useState } from 'react';
import { toast } from 'sonner';
import { RefreshCw, Trash2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { useOfflineSync } from '@/hooks/useOfflineSync';
import {
  layDanhSachCho,
  xoaBanGhiLoi,
  xoaNhatKyCho,
} from '@/lib/offline/farmLogDb';
import type { OfflineEvent } from '@/types/offlineEvent';
import { layNhanHoatDong } from '@/utils/farmLogActivity';

interface FarmLogChoListProps {
  danhSachLo: Array<{ id: string; ten: string }>;
}

type BoLoc = 'all' | 'pending' | 'failed';

const NHAN_TRANG_THAI: Record<string, { label: string; variant: 'secondary' | 'default' | 'destructive' }> = {
  pending: { label: 'Chờ', variant: 'secondary' },
  syncing: { label: 'Đang đồng bộ', variant: 'default' },
  failed: { label: 'Thất bại', variant: 'destructive' },
};

/**
 * Danh sách nhật ký chờ đồng bộ trong IndexedDB (NCL-10-CN-012, MVP).
 */
export const FarmLogChoList: React.FC<FarmLogChoListProps> = ({ danhSachLo }) => {
  const [danhSach, setDanhSach] = useState<OfflineEvent[]>([]);
  const [boLoc, setBoLoc] = useState<BoLoc>('all');
  const { sync, isSyncing } = useOfflineSync();

  const tenLo = useCallback(
    (id?: string) => danhSachLo.find((lo) => lo.id === id)?.ten ?? 'Không xác định',
    [danhSachLo],
  );

  const taiLai = useCallback(async () => {
    try {
      setDanhSach(await layDanhSachCho());
    } catch {
      // IndexedDB lỗi: giữ danh sách cũ
    }
  }, []);

  useEffect(() => {
    taiLai();
    const dinhKy = setInterval(taiLai, 3000);
    return () => clearInterval(dinhKy);
  }, [taiLai]);

  const xuLyXoa = async (id: string) => {
    if (!window.confirm('Bạn có chắc muốn xóa nhật ký này khỏi hàng chờ?')) return;
    await xoaNhatKyCho(id);
    toast.info('Đã xóa nhật ký khỏi hàng chờ.');
    await taiLai();
  };

  const xuLyThuLai = async () => {
    toast.info('Đang thử đồng bộ lại...');
    await sync();
    await taiLai();
  };

  const xuLyXoaLoi = async () => {
    if (!window.confirm('Xóa toàn bộ nhật ký lỗi khỏi hàng chờ?')) return;
    const soXoa = await xoaBanGhiLoi();
    toast.info(`Đã xóa ${soXoa} nhật ký lỗi.`);
    await taiLai();
  };

  const hienThi =
    boLoc === 'all' ? danhSach : danhSach.filter((b) => b.status === boLoc);

  if (danhSach.length === 0) return null;

  const soLoi = danhSach.filter((b) => b.status === 'failed').length;

  return (
    <Card className="mx-auto max-w-md">
      <CardHeader>
        <div className="flex items-center justify-between flex-wrap gap-2">
          <CardTitle className="flex items-center gap-2 text-base">
            <span>Nhật ký chờ đồng bộ</span>
            <Badge variant="default">{danhSach.length}</Badge>
          </CardTitle>
          <div className="flex gap-2">
            <Button variant="outline" size="sm" onClick={xuLyThuLai} disabled={isSyncing}>
              <RefreshCw className={`h-4 w-4 mr-1 ${isSyncing ? 'animate-spin' : ''}`} />
              {isSyncing ? 'Đang đồng bộ...' : 'Thử lại tất cả'}
            </Button>
            {soLoi > 0 && (
              <Button variant="destructive" size="sm" onClick={xuLyXoaLoi} disabled={isSyncing}>
                <Trash2 className="h-4 w-4 mr-1" />
                Xóa bản ghi lỗi
              </Button>
            )}
          </div>
        </div>
        <div className="flex gap-2 pt-1">
          {(
            [
              { value: 'all', label: 'Tất cả' },
              { value: 'pending', label: 'Chờ' },
              { value: 'failed', label: 'Thất bại' },
            ] as Array<{ value: BoLoc; label: string }>
          ).map((muc) => (
            <Button
              key={muc.value}
              variant={boLoc === muc.value ? 'default' : 'ghost'}
              size="sm"
              onClick={() => setBoLoc(muc.value)}
            >
              {muc.label}
            </Button>
          ))}
        </div>
      </CardHeader>
      <CardContent>
        <div className="space-y-1 max-h-96 overflow-y-auto pr-2">
          {hienThi.map((banGhi) => {
            const trangThai = NHAN_TRANG_THAI[banGhi.status ?? 'pending'] ?? NHAN_TRANG_THAI.pending;
            return (
              <div
                key={banGhi.offlineEventId}
                className="flex items-start justify-between border-b pb-2 pt-2"
              >
                <div className="flex-1">
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className="font-medium">
                      {layNhanHoatDong(banGhi.eventData?.activityType)}
                    </span>
                    <Badge variant={trangThai.variant} className="text-xs">
                      {trangThai.label}
                    </Badge>
                    {(banGhi.retryCount ?? 0) > 0 && (
                      <Badge variant="outline" className="text-xs">
                        Thử lần {banGhi.retryCount}
                      </Badge>
                    )}
                  </div>
                  <div className="text-sm text-muted-foreground">
                    Lô: {tenLo(banGhi.productionLotId)}
                  </div>
                  <div className="text-xs text-muted-foreground">
                    Ngày: {String(banGhi.eventData?.executedDate ?? '—')}
                  </div>
                  {banGhi.errorMessage && (
                    <div className="text-xs text-red-500 mt-1">
                      {banGhi.errorMessage}
                    </div>
                  )}
                </div>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => xuLyXoa(banGhi.offlineEventId)}
                  className="text-red-500 hover:text-red-700"
                  disabled={isSyncing}
                  title="Xóa"
                >
                  <Trash2 className="h-4 w-4" />
                </Button>
              </div>
            );
          })}
          {hienThi.length === 0 && (
            <p className="py-4 text-center text-sm text-muted-foreground">
              Không có nhật ký nào ở trạng thái này.
            </p>
          )}
        </div>
      </CardContent>
    </Card>
  );
};
