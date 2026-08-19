import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import { RecordMobileEventForm } from '@/components/mobile/RecordMobileEventForm';
import { getProductionLots } from '@/api/productionLotApi'; // hoặc API riêng
import type { ProductionLot } from '@/types/productionLot';

const RecordMobileEventPage: React.FC = () => {
  const [lots, setLots] = useState<ProductionLot[]>([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    const fetchLots = async () => {
      try {
        // Có thể gọi API lấy tất cả lô và lọc ở frontend, hoặc backend filter
        const data = await getProductionLots();
        // Chỉ giữ lại APPROVED và HARVESTED
        const filtered = data.filter(
          (lot) => lot.status === 'APPROVED' || lot.status === 'HARVESTED'
        );
        setLots(filtered);
      } catch {
        toast.error('Không thể tải danh sách lô');
      } finally {
        setLoading(false);
      }
    };
    fetchLots();
  }, []);

  const handleSuccess = () => navigate('/'); // hoặc dashboard

  if (loading) return <div className="p-8 text-center">Đang tải...</div>;

  return (
    <div className="container max-w-md mx-auto py-4 px-2">
      <RecordMobileEventForm lots={lots} onSuccess={handleSuccess} />
    </div>
  );
};

export default RecordMobileEventPage;