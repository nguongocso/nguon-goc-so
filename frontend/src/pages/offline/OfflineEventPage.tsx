import React from 'react';
import { OfflineEventList } from '@/components/offline/OfflineEventList';

const OfflineEventPage: React.FC = () => {
  return (
    <div className="container mx-auto py-6">
      <div className="mb-4">
        <h1 className="text-2xl font-bold">Quản lý sự kiện chờ đồng bộ</h1>
        <p className="text-muted-foreground text-sm">
          Khi bạn ở vùng không có mạng, sự kiện sẽ được lưu tạm và tự động đồng bộ khi có kết nối.
        </p>
      </div>
      <OfflineEventList />
    </div>
  );
};

export default OfflineEventPage;