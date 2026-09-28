import React, { useCallback, useEffect, useState } from 'react';
import { toast } from 'sonner';
import { Trash2, Download } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { useOfflineSync } from '@/hooks/useOfflineSync';
import {
  getOfflineEvents,
  removeOfflineEvent,
} from '@/services/offlineQueue';
import {
  layLoDuocPhanCong,
  xoaTepTheoNhatKy,
} from '@/lib/offline/farmLogDb';
import { ChainEventType } from '@/enums/chainEventType';
import type { OfflineEvent } from '@/types/offlineEvent';
import { layNhanHoatDong } from '@/utils/farmLogActivity';

interface FarmLogChoListProps {
  /** Danh sách lô để hiển thị tên. Không bắt buộc: tự tải từ cache khi vắng. */
  danhSachLo?: Array<{ id: string; ten: string }>;
}

type BoLoc = 'all' | 'pending' | 'failed' | 'invalid';

const NHAN_TRANG_THAI: Record<string, { label: string; variant: 'secondary' | 'default' | 'destructive' }> = {
  pending: { label: 'Chờ', variant: 'secondary' },
  syncing: { label: 'Đang đồng bộ', variant: 'default' },
  failed: { label: 'Thất bại', variant: 'destructive' },
  invalid: { label: 'Cần xử lý', variant: 'destructive' },
  'da-ghi': { label: 'Đã ghi nội dung, chờ gửi ảnh', variant: 'secondary' },
};

/**
 * Danh sách nhật ký canh tác chờ đồng bộ (NCL-10-CN-012, MVP).
 *
 * Từ v2.4.0 đọc từ HÀNG CHỜ CHUNG (`services/offlineQueue`, lọc `FARM_LOG`)
 * nên bản ghi ngoại tuyến hiện ở cả "Quản lý sự kiện chờ đồng bộ" lẫn danh
 * sách này và tự đồng bộ qua `POST /chain-events/sync`.
 */
export const FarmLogChoList: React.FC<FarmLogChoListProps> = ({ danhSachLo }) => {
  const [danhSach, setDanhSach] = useState<OfflineEvent[]>([]);
  const [boLoc, setBoLoc] = useState<BoLoc>('all');
  const [loCache, setLoCache] = useState<Array<{ id: string; ten: string }>>(
    danhSachLo ?? [],
  );
  // Đồng bộ hoàn toàn tự động (không nút bấm tay); chỉ dùng trạng thái
  // đang đồng bộ để khóa các thao tác xóa trong lúc gửi.
  const { isSyncing } = useOfflineSync();

  // Trang /offline-events không truyền danh sách lô: tự tải từ cache
  // IndexedDB để vẫn hiển thị tên lô.
  useEffect(() => {
    if (danhSachLo && danhSachLo.length > 0) {
      setLoCache(danhSachLo);
      return;
    }
    layLoDuocPhanCong()
      .then((ds) => setLoCache(ds.map((lo) => ({ id: lo.id, ten: lo.ten }))))
      .catch(() => {
        // Cache lỗi: giữ danh sách rỗng, tên hiển thị "Không xác định"
      });
  }, [danhSachLo]);

  const tenLo = useCallback(
    (id?: string) => loCache.find((lo) => lo.id === id)?.ten ?? 'Không xác định',
    [loCache],
  );

  const taiLai = useCallback(() => {
    try {
      const nhatKy = getOfflineEvents()
        .filter((b) => b.eventType === ChainEventType.FARM_LOG)
        .sort((a, b) => b.recordedAt.localeCompare(a.recordedAt));
      setDanhSach(nhatKy);
    } catch {
      // localStorage lỗi: giữ danh sách cũ
    }
  }, []);

  useEffect(() => {
    taiLai();
    const dinhKy = setInterval(taiLai, 3000);
    window.addEventListener('farm-log-queue-changed', taiLai);
    window.addEventListener('offline-queue-changed', taiLai);
    return () => {
      clearInterval(dinhKy);
      window.removeEventListener('farm-log-queue-changed', taiLai);
      window.removeEventListener('offline-queue-changed', taiLai);
    };
  }, [taiLai]);

  const xoaKemAnh = async (id: string) => {
    removeOfflineEvent(id);
    try {
      await xoaTepTheoNhatKy(id);
    } catch {
      // Ảnh dọn sau cũng được, không chặn xóa bản ghi.
    }
  };

  const xuLyXoa = async (id: string) => {
    if (!window.confirm('Bạn có chắc muốn xóa nhật ký này khỏi hàng chờ?')) return;
    await xoaKemAnh(id);
    toast.info('Đã xóa nhật ký khỏi hàng chờ.');
    taiLai();
  };

  const xuLyXoaLoi = async () => {
    if (!window.confirm('Xóa toàn bộ nhật ký lỗi khỏi hàng chờ?')) return;
    const banLoi = getOfflineEvents().filter(
      (b) =>
        b.eventType === ChainEventType.FARM_LOG &&
        (b.status === 'failed' || b.status === 'invalid'),
    );
    for (const banGhi of banLoi) {
      await xoaKemAnh(banGhi.offlineEventId);
    }
    toast.info(`Đã xóa ${banLoi.length} nhật ký lỗi.`);
    taiLai();
  };

  /**
   * Xuất các bản ghi chưa đồng bộ được ra CSV để đối soát thủ công
   * (NCL-10-CN-012 TC-04: bản ghi lỗi được giữ lại kèm lý do).
   */
  const xuLyXuatCsv = () => {
    const danhSachHienTai = getOfflineEvents().filter(
      (b) => b.eventType === ChainEventType.FARM_LOG && b.status !== 'success',
    );
    if (danhSachHienTai.length === 0) {
      toast.info('Không có nhật ký nào cần đối soát.');
      return;
    }
    const dongTieuDe = 'Ma dinh danh,Lo san xuat,Hoat dong,Ngay thuc hien,Vat tu,So luong,Don vi,Trang thai,Ly do';
    const boNgoacKep = (giaTri: unknown) =>
      `"${String(giaTri ?? '').replace(/"/g, '""')}"`;
    const cacDong = danhSachHienTai.map((b) =>
      [
        b.offlineEventId,
        tenLo(b.productionLotId),
        layNhanHoatDong(b.eventData?.activityType),
        b.eventData?.executedDate,
        b.eventData?.material,
        b.eventData?.quantity,
        b.eventData?.unit,
        b.status,
        b.errorMessage,
      ]
        .map(boNgoacKep)
        .join(','),
    );
    const noiDung = `\uFEFF${dongTieuDe}\n${cacDong.join('\n')}`;
    const tep = new Blob([noiDung], { type: 'text/csv;charset=utf-8' });
    const duongDan = URL.createObjectURL(tep);
    const lienKet = document.createElement('a');
    lienKet.href = duongDan;
    lienKet.download = 'nhat-ky-cho-doi-soat.csv';
    document.body.appendChild(lienKet);
    lienKet.click();
    lienKet.remove();
    URL.revokeObjectURL(duongDan);
    toast.success(`Đã xuất ${danhSachHienTai.length} nhật ký chờ ra CSV.`);
  };

  const hienThi =
    boLoc === 'all' ? danhSach : danhSach.filter((b) => b.status === boLoc);

  if (danhSach.length === 0) return null;

  const soLoi = danhSach.filter(
    (b) => b.status === 'failed' || b.status === 'invalid',
  ).length;

  return (
    <Card className="mx-auto max-w-md">
      <CardHeader>
        <div className="flex items-center justify-between flex-wrap gap-2">
          <CardTitle className="flex items-center gap-2 text-base">
            <span>Nhật ký chờ đồng bộ</span>
            <Badge variant="default">{danhSach.length}</Badge>
          </CardTitle>
          <div className="flex gap-2">
            <Button variant="outline" size="sm" onClick={() => void xuLyXuatCsv()}>
              <Download className="h-4 w-4 mr-1" />
              Xuất CSV
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
              { value: 'invalid', label: 'Cần xử lý' },
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
