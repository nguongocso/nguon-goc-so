import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import { AlertTriangle, CloudOff, RefreshCw } from 'lucide-react';
import { RecordFarmLogForm } from '@/components/mobile/RecordFarmLogForm';
import { FarmLogChoList } from '@/components/mobile/FarmLogChoList';
import { MilestoneReminderCard } from '@/components/farm-log/MilestoneReminderCard';
import { Button } from '@/components/ui/button';
import { useOfflineSync } from '@/hooks/useOfflineSync';
import { getProductionLots } from '@/api/productionLotApi';
import type { ProductionLot } from '@/types/productionLot';
import {
  MAX_BAN_GHI_CHO,
  demSoBanGhiCho,
  kiemTraHanDanhMuc,
  layLoDuocPhanCong,
  luuLoDuocPhanCong,
  type HanDanhMuc,
  type LoDuocPhanCong,
} from '@/lib/offline/farmLogDb';

const NGUONG_CANH_BAO_DAY = 80;

/**
 * Trang ghi nhật ký canh tác ngoại tuyến trên mobile (NCL-10-CN-012, MVP).
 *
 * Chỉ được mở trên thiết bị di động thật (bọc `MobileOnlyRoute` ở router).
 * Online → tải lô từ API và làm mới cache; offline → dùng cache IndexedDB.
 */
const RecordFarmLogPage: React.FC = () => {
  const [danhSachLo, setDanhSachLo] = useState<ProductionLot[]>([]);
  const [loCache, setLoCache] = useState<LoDuocPhanCong[]>([]);
  const [hanDanhMuc, setHanDanhMuc] = useState<HanDanhMuc | null>(null);
  const [soBanGhiCho, setSoBanGhiCho] = useState(0);
  const [dangTai, setDangTai] = useState(true);
  const navigate = useNavigate();
  const { isOnline, farmLogPendingCount, isSyncing, sync } = useOfflineSync();

  const taiDuLieu = useCallback(async () => {
    setDangTai(true);
    try {
      if (navigator.onLine) {
        try {
          const data = await getProductionLots();
          const hopLe = data.filter(
            (lot) => lot.status === 'APPROVED' || lot.status === 'HARVESTED',
          );
          setDanhSachLo(hopLe);
          const cache: LoDuocPhanCong[] = hopLe.map((lot) => ({
            id: lot.id,
            ten: lot.name,
            trangThai: lot.status,
          }));
          setLoCache(cache);
          await luuLoDuocPhanCong(cache);
        } catch {
          toast.error('Không thể tải danh sách lô, dùng dữ liệu đã lưu.');
          setLoCache(await layLoDuocPhanCong());
        }
      } else {
        setLoCache(await layLoDuocPhanCong());
      }
    } catch {
      // IndexedDB lỗi: vẫn hiển thị form, form sẽ báo khi lưu tạm
    } finally {
      try {
        setHanDanhMuc(await kiemTraHanDanhMuc());
        setSoBanGhiCho(await demSoBanGhiCho());
      } catch {
        // Bỏ qua
      }
      setDangTai(false);
    }
  }, []);

  useEffect(() => {
    taiDuLieu();
  }, [taiDuLieu]);

  // Khi có mạng trở lại: làm mới cache lô để gia hạn 7 ngày.
  useEffect(() => {
    if (isOnline) taiDuLieu();
  }, [isOnline, taiDuLieu]);

  const loHienThi = isOnline
    ? danhSachLo.map((lot) => ({ id: lot.id, ten: lot.name }))
    : loCache;

  const hetHanCache = !isOnline && hanDanhMuc && !hanDanhMuc.conHan;
  const hangChoDay = soBanGhiCho >= MAX_BAN_GHI_CHO;
  const chanForm = hetHanCache || hangChoDay || (!isOnline && loHienThi.length === 0);

  if (dangTai) return <div className="p-8 text-center">Đang tải...</div>;

  return (
    <div className="container max-w-md mx-auto py-4 px-2 space-y-4">
      {!isOnline && (
        <div className="flex items-center gap-2 rounded-lg border border-amber-300 bg-amber-50 p-3 text-sm text-amber-800">
          <CloudOff className="h-4 w-4 shrink-0" />
          <span>Đang ngoại tuyến — bản ghi sẽ được lưu tạm trên thiết bị.</span>
        </div>
      )}

      {farmLogPendingCount > 0 && isOnline && (
        <div className="flex items-center justify-between gap-2 rounded-lg border border-sky-300 bg-sky-50 p-3 text-sm text-sky-800">
          <span>Có {farmLogPendingCount} nhật ký chờ đồng bộ.</span>
          <Button size="sm" variant="outline" onClick={() => sync()} disabled={isSyncing}>
            <RefreshCw className={`h-4 w-4 mr-1 ${isSyncing ? 'animate-spin' : ''}`} />
            Đồng bộ ngay
          </Button>
        </div>
      )}

      {hetHanCache && (
        <div className="flex items-center gap-2 rounded-lg border border-red-300 bg-red-50 p-3 text-sm text-red-800">
          <AlertTriangle className="h-4 w-4 shrink-0" />
          <span>
            Dữ liệu lô đã hết hạn (quá 7 ngày). Vui lòng kết nối mạng để tải lại
            trước khi ghi.
          </span>
        </div>
      )}

      {!isOnline && hanDanhMuc?.conHan && hanDanhMuc.soNgayConLai <= 2 && (
        <div className="flex items-center gap-2 rounded-lg border border-amber-300 bg-amber-50 p-3 text-sm text-amber-800">
          <AlertTriangle className="h-4 w-4 shrink-0" />
          <span>
            Dữ liệu lô còn {hanDanhMuc.soNgayConLai} ngày hiệu lực. Hãy đồng bộ
            sớm khi có mạng.
          </span>
        </div>
      )}

      {hangChoDay && (
        <div className="flex items-center gap-2 rounded-lg border border-red-300 bg-red-50 p-3 text-sm text-red-800">
          <AlertTriangle className="h-4 w-4 shrink-0" />
          <span>
            Hàng chờ đã đầy ({MAX_BAN_GHI_CHO} bản ghi). Vui lòng kết nối mạng để
            đồng bộ trước khi ghi tiếp.
          </span>
        </div>
      )}

      {!hangChoDay && soBanGhiCho >= NGUONG_CANH_BAO_DAY && (
        <div className="flex items-center gap-2 rounded-lg border border-amber-300 bg-amber-50 p-3 text-sm text-amber-800">
          <AlertTriangle className="h-4 w-4 shrink-0" />
          <span>Hàng chờ sắp đầy ({soBanGhiCho}/{MAX_BAN_GHI_CHO}).</span>
        </div>
      )}

      <MilestoneReminderCard userOnly={true} />

      {chanForm ? (
        <div className="rounded-lg border p-6 text-center text-sm text-muted-foreground">
          Chưa thể ghi nhật ký lúc này. Vui lòng kết nối mạng để tiếp tục.
        </div>
      ) : (
        <RecordFarmLogForm
          danhSachLo={loHienThi}
          isOnline={isOnline}
          onSuccess={() => navigate('/')}
        />
      )}

      <FarmLogChoList danhSachLo={isOnline ? danhSachLo.map((lot) => ({ id: lot.id, ten: lot.name })) : loCache} />
    </div>
  );
};

export default RecordFarmLogPage;
