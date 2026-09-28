import React from 'react';
import { Link } from 'react-router-dom';
import { OfflineEventList } from '@/components/offline/OfflineEventList';
import { HelpButton } from '@/components/help/HelpButton';
import { useOfflineSync } from '@/hooks/useOfflineSync';

const OfflineEventPage: React.FC = () => {
  // Trang quản lý tất cả sự kiện chờ đồng bộ (v2.4.0):
  // Một hàng chờ chung (localStorage), một danh sách duy nhất qua
  // OfflineEventList (đã render cả dòng FARM_LOG kèm tên lô).
  // Ghi nhật ký ngoại tuyến xếp vào hàng chờ chung nên `pendingCount` đã bao
  // gồm nhật ký canh tác.
  const { pendingCount } = useOfflineSync();
  const tongCho = pendingCount;

  return (
    <div className="space-y-6">

      <div className="mb-4 flex items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">Quản lý sự kiện chờ đồng bộ</h1>
          <p className="text-muted-foreground text-sm">
            Khi bạn ở vùng không có mạng, sự kiện sẽ được lưu tạm và tự động đồng bộ khi có kết nối.
            Trang này quản lý tất cả hàng chờ{tongCho > 0 ? ` (hiện có ${tongCho} mục)` : ''}:
            sự kiện chuỗi cung ứng và nhật ký canh tác ngoại tuyến.
            Ghi nhật ký khi ngoại tuyến tại <Link to="/farm-logs/create" className="font-medium text-emerald-700 underline">Ghi nhật ký canh tác</Link>.
          </p>
        </div>
        <HelpButton screenKey="offline-events" />
      </div>
      <OfflineEventList />
    </div>
  );
};

export default OfflineEventPage;